package dao;

import model.User;
import util.Db;
import util.Table;
import java.sql.*;
import java.util.Optional;

public final class UserDao {
    private UserDao() {}
    private static User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("user_id"), rs.getString("full_name"), rs.getString("email"),
            rs.getString("phone"), rs.getString("role"));
    }
    public static Optional<User> login(String email, String hash) throws SQLException {
        return Db.list("SELECT user_id, full_name, email, phone, role FROM users WHERE email=? AND password_hash=?",
            UserDao::map, email, hash).stream().findFirst();
    }
    public static int register(String name, String email, String phone, String hash) throws SQLException {
        return Db.insert("INSERT INTO users (full_name, email, phone, password_hash, role) VALUES (?,?,?,?, 'CUSTOMER')",
            name, email, phone, hash);
    }
    public static Table all() throws SQLException {
        return Db.query("""
            SELECT u.user_id AS ID, u.full_name AS Name, u.email AS Email, u.phone AS Phone, u.role AS Role,
                   u.created_at AS Registered, COUNT(b.booking_id) AS Bookings
            FROM users u LEFT JOIN bookings b ON b.user_id = u.user_id
            GROUP BY u.user_id, u.full_name, u.email, u.phone, u.role, u.created_at
            ORDER BY u.user_id""");
    }
}
