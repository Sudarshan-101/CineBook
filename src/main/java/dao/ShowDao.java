package dao;

import model.Show;
import util.Db;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.List;

public final class ShowDao {
    private ShowDao() {}
    private static Show map(ResultSet rs) throws SQLException {
        return new Show(rs.getInt("show_id"), rs.getInt("movie_id"), rs.getInt("screen_id"), rs.getInt("cinema_id"),
            rs.getDate("show_date").toLocalDate(), rs.getTime("show_time").toLocalTime(), rs.getBigDecimal("price"),
            rs.getString("title"), rs.getString("cinema_name"), rs.getString("screen_name"));
    }
    public static List<Show> all() throws SQLException {
        return Db.list("SELECT * FROM v_show_details ORDER BY show_date, show_time", ShowDao::map);
    }
    public static List<Show> upcomingForMovie(int movieId) throws SQLException {
        return Db.list("""
            SELECT * FROM v_show_details
            WHERE movie_id = ? AND TIMESTAMP(show_date, show_time) > NOW()
            ORDER BY show_date, show_time""", ShowDao::map, movieId);
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
