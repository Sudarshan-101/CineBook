package dao;

import model.Cinema;
import util.Db;
import java.sql.SQLException;
import java.util.List;

public final class CinemaDao {
    private CinemaDao() {}
    public static List<Cinema> all() throws SQLException {
        return Db.list("SELECT * FROM cinemas ORDER BY city, name",
            rs -> new Cinema(rs.getInt("cinema_id"), rs.getString("name"), rs.getString("city"), rs.getString("address")));
    }
    public static void insert(String name, String city, String address) throws SQLException {
        Db.insert("INSERT INTO cinemas (name, city, address) VALUES (?,?,?)", name, city, address);
    }
    public static void update(int id, String name, String city, String address) throws SQLException {
        Db.update("UPDATE cinemas SET name=?, city=?, address=? WHERE cinema_id=?", name, city, address, id);
    }
    public static void delete(int id) throws SQLException { Db.update("DELETE FROM cinemas WHERE cinema_id=?", id); }
}
