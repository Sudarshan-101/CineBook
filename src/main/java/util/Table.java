package util;

import java.sql.*;
import java.util.*;

/** Simple in-memory result set: column names + rows. */
public record Table(String[] cols, List<Object[]> rows) {
    public static Table from(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        String[] cols = new String[n];
        for (int i = 0; i < n; i++) cols[i] = md.getColumnLabel(i + 1);
        List<Object[]> rows = new ArrayList<>();
        while (rs.next()) {
            Object[] r = new Object[n];
            for (int i = 0; i < n; i++) r[i] = rs.getObject(i + 1);
            rows.add(r);
        }
        return new Table(cols, rows);
    }
}
