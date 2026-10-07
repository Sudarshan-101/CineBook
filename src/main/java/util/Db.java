package util;

import java.sql.*;
import java.util.*;

/** JDBC helper. Configure with env vars CINEMA_DB_URL / CINEMA_DB_USER / CINEMA_DB_PASSWORD. */
public final class Db {
    private Db() {}

    private static String env(String k, String d) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? d : v;
    }
    private static final String URL = env("CINEMA_DB_URL",
        "jdbc:mysql://localhost:3306/cinema_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    private static final String USER = env("CINEMA_DB_USER", "root");
    private static final String PASS = env("CINEMA_DB_PASSWORD", "");

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public interface RowMapper<T> { T map(ResultSet rs) throws SQLException; }

    private static void bind(PreparedStatement ps, Object... p) throws SQLException {
        for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
    }

    public static Table query(String sql, Object... p) throws SQLException {
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            try (ResultSet rs = ps.executeQuery()) { return Table.from(rs); }
        }
    }

    public static <T> List<T> list(String sql, RowMapper<T> m, Object... p) throws SQLException {
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(m.map(rs));
                return out;
            }
        }
    }

    public static int update(String sql, Object... p) throws SQLException {
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            return ps.executeUpdate();
        }
    }

    /** Executes an INSERT and returns the generated key. */
    public static int insert(String sql, Object... p) throws SQLException {
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, p);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }
}
