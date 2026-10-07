package dao;

import model.Seat;
import util.Db;
import java.sql.*;
import java.util.*;

public final class SeatDao {
    private SeatDao() {}

    public static List<Seat> byScreen(int screenId) throws SQLException {
        return Db.list("SELECT * FROM seats WHERE screen_id=? ORDER BY seat_row, seat_number",
            rs -> new Seat(rs.getInt("seat_id"), rs.getInt("screen_id"), rs.getString("seat_row"),
                rs.getInt("seat_number"), rs.getString("seat_type")), screenId);
    }

    /** Seat ids already taken (CONFIRMED bookings) for a show. */
    public static Set<Integer> booked(int showId) throws SQLException {
        return new HashSet<>(Db.list("""
            SELECT bs.seat_id FROM booking_seats bs JOIN bookings b ON b.booking_id = bs.booking_id
            WHERE b.show_id = ? AND b.status = 'CONFIRMED'""", rs -> rs.getInt(1), showId));
    }

    public static void add(int screenId, String row, int number, String type) throws SQLException {
        Db.insert("INSERT INTO seats (screen_id, seat_row, seat_number, seat_type) VALUES (?,?,?,?)",
            screenId, row.toUpperCase(), number, type);
    }
    public static void updateType(int id, String type) throws SQLException {
        Db.update("UPDATE seats SET seat_type=? WHERE seat_id=?", type, id);
    }
    public static void delete(int id) throws SQLException { Db.update("DELETE FROM seats WHERE seat_id=?", id); }

    /** Generates a rows x perRow grid inside ONE transaction; the last premiumRows rows are PREMIUM. */
    public static int generate(int screenId, int rows, int perRow, int premiumRows) throws SQLException {
        if (rows < 1 || rows > 26 || perRow < 1 || perRow > 30)
            throw new IllegalArgumentException("Rows must be 1-26 and seats per row 1-30.");
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT IGNORE INTO seats (screen_id, seat_row, seat_number, seat_type) VALUES (?,?,?,?)")) {
                for (int r = 0; r < rows; r++)
                    for (int n = 1; n <= perRow; n++) {
                        ps.setInt(1, screenId);
                        ps.setString(2, String.valueOf((char) ('A' + r)));
                        ps.setInt(3, n);
                        ps.setString(4, r >= rows - premiumRows ? "PREMIUM" : "REGULAR");
                        ps.addBatch();
                    }
                int[] res = ps.executeBatch();
                c.commit();
                int count = 0;
                for (int x : res) if (x > 0 || x == Statement.SUCCESS_NO_INFO) count++;
                return count;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }
}
