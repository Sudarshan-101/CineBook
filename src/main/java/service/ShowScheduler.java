package service;

import util.Db;
import java.sql.*;
import java.time.*;
import java.util.*;

/**
 * Keeps the timetable full. On every start-up it makes sure that every day from today to
 * today + daysAhead has a sensible number of shows, so the "Browse & Book" screen never runs dry
 * (shows older than "now" are hidden automatically).
 *
 * Uses INSERT IGNORE: the UNIQUE (screen_id, show_date, show_time) key means existing shows are never touched.
 */
public final class ShowScheduler {
    private ShowScheduler() {}

    private static final LocalTime[] SLOTS = {
        LocalTime.of(10, 0), LocalTime.of(13, 30), LocalTime.of(17, 0), LocalTime.of(20, 30)
    };
    private static final int MIN_SHOWS_PER_DAY = 6;

    /** @return number of shows created */
    public static int ensure(int daysAhead) throws SQLException {
        List<Integer> movies = Db.list("SELECT movie_id FROM movies ORDER BY movie_id", rs -> rs.getInt(1));
        List<int[]> screens = Db.list("SELECT screen_id, cinema_id FROM screens ORDER BY screen_id",
            rs -> new int[]{rs.getInt(1), rs.getInt(2)});
        if (movies.isEmpty() || screens.isEmpty()) return 0;

        LocalDate today = LocalDate.now();
        int created = 0;
        try (Connection c = Db.get()) {
            c.setAutoCommit(false);
            try (PreparedStatement count = c.prepareStatement("SELECT COUNT(*) FROM shows WHERE show_date = ?");
                 PreparedStatement ins = c.prepareStatement(
                     "INSERT IGNORE INTO shows (movie_id, screen_id, show_date, show_time, price) VALUES (?,?,?,?,?)")) {
                for (int d = 0; d <= daysAhead; d++) {
                    LocalDate date = today.plusDays(d);
                    count.setDate(1, java.sql.Date.valueOf(date));
                    try (ResultSet rs = count.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) >= MIN_SHOWS_PER_DAY) continue;
                    }
                    for (int si = 0; si < screens.size(); si++) {
                        int screenId = screens.get(si)[0], cinemaId = screens.get(si)[1];
                        for (int k = 0; k < SLOTS.length; k++) {
                            LocalTime t = SLOTS[k];
                            if (d == 0 && !t.isAfter(LocalTime.now().plusMinutes(30))) continue;   // already gone
                            int movieId = movies.get(Math.floorMod(si * 3 + k * 5 + d * 2, movies.size()));
                            int price = 180 + (cinemaId % 3) * 30 + (k == 2 ? 40 : k == 3 ? 70 : 0);
                            ins.setInt(1, movieId);
                            ins.setInt(2, screenId);
                            ins.setDate(3, java.sql.Date.valueOf(date));
                            ins.setTime(4, java.sql.Time.valueOf(t));
                            ins.setInt(5, price);
                            ins.addBatch();
                        }
                    }
                }
                for (int n : ins.executeBatch()) if (n > 0 || n == Statement.SUCCESS_NO_INFO) created++;
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
        return created;
    }
}
