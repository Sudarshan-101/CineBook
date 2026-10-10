package dao;

import model.Show;
import util.Db;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;

public final class ShowDao {
    private ShowDao() {}
    private static Show map(ResultSet rs) throws SQLException {
        return new Show(rs.getInt("show_id"), rs.getInt("movie_id"), rs.getInt("screen_id"), rs.getInt("cinema_id"),
            rs.getObject("show_date", LocalDate.class), rs.getObject("show_time", LocalTime.class), rs.getBigDecimal("price"),
            rs.getString("title"), rs.getString("cinema_name"), rs.getString("screen_name"));
    }
    public static List<Show> all() throws SQLException {
        return Db.list("SELECT * FROM v_show_details ORDER BY show_date, show_time", ShowDao::map);
    }
    public static List<Show> upcomingForMovie(int movieId) throws SQLException {
        return Db.list("""
            SELECT * FROM v_show_details
            WHERE movie_id = ? AND TIMESTAMP(show_date, show_time) > ?
            ORDER BY show_date, show_time""", ShowDao::map, movieId, LocalDateTime.now());
    }
    /** show_id -> {seatsBooked, seatsTotal} for every show of a movie (used for the "seats left" badges). */
    public static Map<Integer, int[]> availability(int movieId) throws SQLException {
        Map<Integer, int[]> out = new HashMap<>();
        for (int[] a : Db.list("""
            SELECT sh.show_id,
                   (SELECT COUNT(*) FROM booking_seats bs JOIN bookings b ON b.booking_id = bs.booking_id
                     WHERE b.show_id = sh.show_id AND b.status = 'CONFIRMED') AS booked,
                   (SELECT COUNT(*) FROM seats se WHERE se.screen_id = sh.screen_id) AS total
            FROM shows sh WHERE sh.movie_id = ?""",
                rs -> new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3)}, movieId))
            out.put(a[0], new int[]{a[1], a[2]});
        return out;
    }
    public static void insert(int movieId, int screenId, LocalDate d, LocalTime t, BigDecimal price) throws SQLException {
        Db.insert("INSERT INTO shows (movie_id, screen_id, show_date, show_time, price) VALUES (?,?,?,?,?)",
            movieId, screenId, d, t, price);
    }
    public static void update(int id, int movieId, int screenId, LocalDate d, LocalTime t, BigDecimal price) throws SQLException {
        Db.update("UPDATE shows SET movie_id=?, screen_id=?, show_date=?, show_time=?, price=? WHERE show_id=?",
            movieId, screenId, d, t, price, id);
    }
    public static void delete(int id) throws SQLException { Db.update("DELETE FROM shows WHERE show_id=?", id); }
}
