package service;

import util.Db;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Booking logic. Every operation is ONE database transaction (ACID):
 *   lock show row -> validate seats -> insert booking -> insert booking_seats -> insert payment -> COMMIT
 * Any failure triggers ROLLBACK, so nothing is half-saved.
 *
 * Double booking is prevented by (1) SELECT ... FOR UPDATE on the show row, which serialises
 * concurrent bookings of the same show, (2) an availability check inside the lock, and
 * (3) a database trigger as a last line of defence.
 */
public final class BookingService {
    private BookingService() {}
    private static final BigDecimal PREMIUM_FACTOR = new BigDecimal("1.5");

    public static BigDecimal seatPrice(BigDecimal base, String seatType) {
        BigDecimal p = "PREMIUM".equals(seatType) ? base.multiply(PREMIUM_FACTOR) : base;
        return p.setScale(2, RoundingMode.HALF_UP);
    }

    public static int book(int userId, int showId, List<Integer> seatIdsIn, String method, boolean failPayment)
            throws BookingException {
        List<Integer> seatIds = new ArrayList<>(new LinkedHashSet<>(seatIdsIn));
        if (seatIds.isEmpty()) throw new BookingException("Select at least one seat.");
        if (seatIds.size() > 10) throw new BookingException("You can book at most 10 seats at once.");

        Connection c = null;
        try {
            c = Db.get();
            c.setAutoCommit(false);                                   // BEGIN
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            // 1. Lock the show row: concurrent bookings of this show queue up here
            int screenId = 0;
            BigDecimal base = null;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT screen_id, price, show_date, show_time FROM shows WHERE show_id = ? FOR UPDATE")) {
                ps.setInt(1, showId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new BookingException("This show no longer exists.");
                    screenId = rs.getInt(1);
                    base = rs.getBigDecimal(2);
                    LocalDateTime start = LocalDateTime.of(rs.getObject(3, java.time.LocalDate.class), rs.getObject(4, java.time.LocalTime.class));
                    if (start.isBefore(LocalDateTime.now())) throw new BookingException("This show has already started.");
                }
            }

            // 2. Validate seats belong to the show's screen and compute price
            Map<Integer, BigDecimal> prices = new LinkedHashMap<>();
            BigDecimal total = BigDecimal.ZERO;
            try (PreparedStatement ps = c.prepareStatement("SELECT seat_type FROM seats WHERE seat_id = ? AND screen_id = ?")) {
                for (int sid : seatIds) {
                    ps.setInt(1, sid);
                    ps.setInt(2, screenId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new BookingException("Invalid seat selected for this show.");
                        BigDecimal p = seatPrice(base, rs.getString(1));
                        prices.put(sid, p);
                        total = total.add(p);
                    }
                }
            }

            // 3. Availability check (safe: we hold the show lock)
            String in = String.join(",", Collections.nCopies(seatIds.size(), "?"));
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT CONCAT(se.seat_row, se.seat_number)
                    FROM booking_seats bs
                    JOIN bookings b ON b.booking_id = bs.booking_id
                    JOIN seats se ON se.seat_id = bs.seat_id
                    WHERE b.show_id = ? AND b.status = 'CONFIRMED' AND bs.seat_id IN (""" + in + ")")) {
                ps.setInt(1, showId);
                for (int i = 0; i < seatIds.size(); i++) ps.setInt(i + 2, seatIds.get(i));
                try (ResultSet rs = ps.executeQuery()) {
                    List<String> taken = new ArrayList<>();
                    while (rs.next()) taken.add(rs.getString(1));
                    if (!taken.isEmpty())
                        throw new BookingException("Seat(s) already booked by someone else: " + String.join(", ", taken)
                            + ". Please choose different seats.");
                }
            }

            // 4. Booking header
            int bookingId;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO bookings (user_id, show_id, total_amount, status) VALUES (?,?,?, 'CONFIRMED')",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setInt(2, showId);
                ps.setBigDecimal(3, total);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { k.next(); bookingId = k.getInt(1); }
            }

            // 5. Booking seats (composite PK booking_id + seat_id)
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO booking_seats (booking_id, seat_id, price) VALUES (?,?,?)")) {
                for (var e : prices.entrySet()) {
                    ps.setInt(1, bookingId);
                    ps.setInt(2, e.getKey());
                    ps.setBigDecimal(3, e.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // 6. Payment (simulated). A failure here undoes steps 4 and 5 as well.
            if (failPayment)
                throw new BookingException("Payment failed (simulated). The transaction was ROLLED BACK - no booking or seats were saved.");
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO payments (booking_id, amount, method, status) VALUES (?,?,?, 'SUCCESS')")) {
                ps.setInt(1, bookingId);
                ps.setBigDecimal(2, total);
                ps.setString(3, method);
                ps.executeUpdate();
            }

            c.commit();                                               // COMMIT
            return bookingId;
        } catch (BookingException e) {
            rollback(c);
            throw e;
        } catch (SQLException e) {
            rollback(c);
            String m = "45000".equals(e.getSQLState()) ? e.getMessage() : "Database error: " + e.getMessage();
            throw new BookingException(m);
        } finally {
            close(c);
        }
    }

    /** Cancels a CONFIRMED booking of a future show and refunds the payment (one transaction). */
    public static void cancel(int bookingId, int userId) throws BookingException {
        Connection c = null;
        try {
            c = Db.get();
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT b.status, sh.show_date, sh.show_time FROM bookings b JOIN shows sh ON sh.show_id = b.show_id
                    WHERE b.booking_id = ? AND b.user_id = ? FOR UPDATE""")) {
                ps.setInt(1, bookingId);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new BookingException("Booking not found.");
                    if (!"CONFIRMED".equals(rs.getString(1))) throw new BookingException("Booking is already cancelled.");
                    if (LocalDateTime.of(rs.getObject(2, java.time.LocalDate.class), rs.getObject(3, java.time.LocalTime.class)).isBefore(LocalDateTime.now()))
                        throw new BookingException("Cannot cancel - the show has already started.");
                }
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE bookings SET status = 'CANCELLED' WHERE booking_id = ?")) {
                ps.setInt(1, bookingId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE payments SET status = 'REFUNDED' WHERE booking_id = ?")) {
                ps.setInt(1, bookingId);
                ps.executeUpdate();
            }
            c.commit();
        } catch (BookingException e) {
            rollback(c);
            throw e;
        } catch (SQLException e) {
            rollback(c);
            throw new BookingException("Database error: " + e.getMessage());
        } finally {
            close(c);
        }
    }

    private static void rollback(Connection c) { try { if (c != null) c.rollback(); } catch (SQLException ignored) {} }
    private static void close(Connection c) { try { if (c != null) c.close(); } catch (SQLException ignored) {} }
}
