package dao;

import model.Screen;
import util.Db;
import java.sql.SQLException;
import java.util.List;

public final class ScreenDao {
    private ScreenDao() {}
    public static List<Screen> all() throws SQLException {
        return Db.list("""
            SELECT s.screen_id, s.cinema_id, s.screen_name, c.name AS cinema_name
            FROM screens s JOIN cinemas c ON c.cinema_id = s.cinema_id ORDER BY c.name, s.screen_name""",
            rs -> new Screen(rs.getInt("screen_id"), rs.getInt("cinema_id"), rs.getString("screen_name"), rs.getString("cinema_name")));
    }
    public static void insert(int cinemaId, String name) throws SQLException {
        Db.insert("INSERT INTO screens (cinema_id, screen_name) VALUES (?,?)", cinemaId, name);
    }
    public static void update(int id, int cinemaId, String name) throws SQLException {
        Db.update("UPDATE screens SET cinema_id=?, screen_name=? WHERE screen_id=?", cinemaId, name, id);
    }
    public static void delete(int id) throws SQLException { Db.update("DELETE FROM screens WHERE screen_id=?", id); }
}
