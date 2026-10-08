package dao;

import model.Booking;
import util.Db;
import util.Table;
import java.sql.SQLException;
import java.util.List;

public final class BookingDao {
    private BookingDao() {}

    public static List<Booking> forUser(int userId) throws SQLException {
        return Db.list("""
            SELECT b.booking_id, v.title, v.cinema_name, v.screen_name, v.show_date, v.show_time,
                   (SELECT GROUP_CONCAT(CONCAT(se.seat_row, se.seat_number) ORDER BY se.seat_row, se.seat_number SEPARATOR ', ')
                      FROM booking_seats bs JOIN seats se ON se.seat_id = bs.seat_id
                     WHERE bs.booking_id = b.booking_id) AS seats,
                   b.total_amount, b.status
            FROM bookings b JOIN v_show_details v ON v.show_id = b.show_id
            WHERE b.user_id = ? ORDER BY b.booking_time DESC""",
            rs -> new Booking(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getDate(5).toLocalDate(), rs.getTime(6).toLocalTime(), rs.getString(7),
                rs.getBigDecimal(8), rs.getString(9)), userId);
    }

    /** Admin view: every booking with customer, show, seats and payment info. */
    public static Table all() throws SQLException {
        return Db.query("""
            SELECT b.booking_id AS ID, u.full_name AS Customer, v.title AS Movie, v.cinema_name AS Cinema,
                   v.screen_name AS Screen, v.show_date AS Date, v.show_time AS Time,
                   GROUP_CONCAT(CONCAT(se.seat_row, se.seat_number) ORDER BY se.seat_row, se.seat_number SEPARATOR ', ') AS Seats,
                   b.total_amount AS Total, p.method AS Method, p.status AS Payment, b.status AS Status
            FROM bookings b
            JOIN users u ON u.user_id = b.user_id
            JOIN v_show_details v ON v.show_id = b.show_id
            JOIN booking_seats bs ON bs.booking_id = b.booking_id
            JOIN seats se ON se.seat_id = bs.seat_id
            LEFT JOIN payments p ON p.booking_id = b.booking_id
            GROUP BY b.booking_id, u.full_name, v.title, v.cinema_name, v.screen_name, v.show_date, v.show_time,
                     b.total_amount, p.method, p.status, b.status, b.booking_time
            ORDER BY b.booking_time DESC""");
    }
}
