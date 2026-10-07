package dao;

import util.Db;
import util.Table;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Revenue / analytics reports. Each report is a plain SQL query (shown in the UI for the viva). */
public final class ReportDao {
    private ReportDao() {}

    public static final Map<String, String> REPORTS = new LinkedHashMap<>();
    static {
        REPORTS.put("Revenue by movie", """
            SELECT m.title AS Movie, COUNT(DISTINCT b.booking_id) AS Bookings, COUNT(bs.seat_id) AS Tickets,
                   SUM(bs.price) AS Revenue
            FROM bookings b
            JOIN booking_seats bs ON bs.booking_id = b.booking_id
            JOIN shows sh ON sh.show_id = b.show_id
            JOIN movies m ON m.movie_id = sh.movie_id
            WHERE b.status = 'CONFIRMED'
            GROUP BY m.movie_id, m.title
            ORDER BY Revenue DESC""");
        REPORTS.put("Revenue by cinema", """
            SELECT c.name AS Cinema, c.city AS City, COUNT(DISTINCT b.booking_id) AS Bookings,
                   COUNT(bs.seat_id) AS Tickets, SUM(bs.price) AS Revenue
            FROM bookings b
            JOIN booking_seats bs ON bs.booking_id = b.booking_id
            JOIN shows sh ON sh.show_id = b.show_id
            JOIN screens sc ON sc.screen_id = sh.screen_id
            JOIN cinemas c ON c.cinema_id = sc.cinema_id
            WHERE b.status = 'CONFIRMED'
            GROUP BY c.cinema_id, c.name, c.city
            ORDER BY Revenue DESC""");
        REPORTS.put("Daily revenue", """
            SELECT DATE(b.booking_time) AS Day, COUNT(*) AS Bookings, SUM(b.total_amount) AS Revenue
            FROM bookings b
            WHERE b.status = 'CONFIRMED'
            GROUP BY DATE(b.booking_time)
            ORDER BY Day DESC""");
        REPORTS.put("Popular movies (HAVING >= 2 tickets)", """
            SELECT m.title AS Movie, COUNT(bs.seat_id) AS Tickets
            FROM booking_seats bs
            JOIN bookings b ON b.booking_id = bs.booking_id AND b.status = 'CONFIRMED'
            JOIN shows sh ON sh.show_id = b.show_id
            JOIN movies m ON m.movie_id = sh.movie_id
            GROUP BY m.movie_id, m.title
            HAVING COUNT(bs.seat_id) >= 2
            ORDER BY Tickets DESC""");
        REPORTS.put("Top customers (spent above average - subquery)", """
            SELECT u.full_name AS Customer, SUM(b.total_amount) AS Spent
            FROM users u JOIN bookings b ON b.user_id = u.user_id
            WHERE b.status = 'CONFIRMED'
            GROUP BY u.user_id, u.full_name
            HAVING SUM(b.total_amount) > (
                SELECT AVG(t.total) FROM (
                    SELECT SUM(total_amount) AS total FROM bookings WHERE status = 'CONFIRMED' GROUP BY user_id) t)
            ORDER BY Spent DESC""");
        REPORTS.put("Seat occupancy per show", """
            SELECT v.show_id AS ID, v.title AS Movie, v.cinema_name AS Cinema, v.show_date AS Date, v.show_time AS Time,
                   COUNT(bs.seat_id) AS Booked,
                   (SELECT COUNT(*) FROM seats s WHERE s.screen_id = v.screen_id) AS Capacity,
                   ROUND(100 * COUNT(bs.seat_id) / (SELECT COUNT(*) FROM seats s WHERE s.screen_id = v.screen_id), 1) AS `Occupancy %`
            FROM v_show_details v
            LEFT JOIN bookings b ON b.show_id = v.show_id AND b.status = 'CONFIRMED'
            LEFT JOIN booking_seats bs ON bs.booking_id = b.booking_id
            GROUP BY v.show_id, v.title, v.cinema_name, v.show_date, v.show_time, v.screen_id
            ORDER BY v.show_date, v.show_time""");
    }

    public static Table run(String name) throws SQLException { return Db.query(REPORTS.get(name)); }

    public static Table summary() throws SQLException {
        return Db.query("""
            SELECT (SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER') AS customers,
                   (SELECT COUNT(*) FROM bookings WHERE status = 'CONFIRMED') AS bookings,
                   (SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'SUCCESS') AS revenue,
                   (SELECT COUNT(*) FROM movies) AS movies""");
    }
}
