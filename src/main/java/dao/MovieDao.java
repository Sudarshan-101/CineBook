package dao;

import model.Movie;
import util.Db;
import java.sql.*;
import java.util.List;

public final class MovieDao {
    private MovieDao() {}
    private static Movie map(ResultSet rs) throws SQLException {
        java.time.LocalDate d = rs.getObject("release_date", java.time.LocalDate.class);
        return new Movie(rs.getInt("movie_id"), rs.getString("title"), rs.getString("genre"), rs.getString("language"),
            rs.getInt("duration_min"), rs.getString("rating"), d, rs.getString("description"));
    }
    public static List<Movie> search(String q) throws SQLException {
        String like = "%" + q + "%";
        return Db.list("SELECT * FROM movies WHERE title LIKE ? OR genre LIKE ? OR language LIKE ? ORDER BY title",
            MovieDao::map, like, like, like);
    }
    public static List<String> genres() throws SQLException {
        return Db.list("SELECT DISTINCT genre FROM movies WHERE genre IS NOT NULL AND genre <> '' ORDER BY genre", rs -> rs.getString(1));
    }
    public static List<Movie> all() throws SQLException { return search(""); }
    public static void insert(Movie m) throws SQLException {
        Db.insert("INSERT INTO movies (title, genre, language, duration_min, rating, release_date, description) VALUES (?,?,?,?,?,?,?)",
            m.title(), m.genre(), m.language(), m.duration(), m.rating(), m.releaseDate(), m.description());
    }
    public static void update(Movie m) throws SQLException {
        Db.update("UPDATE movies SET title=?, genre=?, language=?, duration_min=?, rating=?, release_date=?, description=? WHERE movie_id=?",
            m.title(), m.genre(), m.language(), m.duration(), m.rating(), m.releaseDate(), m.description(), m.id());
    }
    public static void delete(int id) throws SQLException { Db.update("DELETE FROM movies WHERE movie_id=?", id); }
}
