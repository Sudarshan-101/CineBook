package ui;

import dao.*;
import model.*;
import util.Db;
import util.Table;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.*;

import static ui.FormDialog.s;

/** Admin CRUD tabs. */
public final class AdminPanels {
    private AdminPanels() {}
    private static final String[] TYPES = {"REGULAR", "PREMIUM"};

    // ------------------------------------------------------------ MOVIES
    public static class MoviesPanel extends CrudPanel {
        private List<Movie> cache = List.of();
        public MoviesPanel() { super(true, true, true); }

        @Override protected Table load() throws Exception {
            cache = MovieDao.all();
            List<Object[]> rows = new ArrayList<>();
            for (Movie m : cache)
                rows.add(new Object[]{m.id(), m.title(), m.genre(), m.language(), m.duration(), m.rating(), m.releaseDate(), m.description()});
            return new Table(new String[]{"ID", "Title", "Genre", "Language", "Duration (min)", "Rating", "Release", "Description"}, rows);
        }
        private Movie ask(Movie m) {
            var v = FormDialog.show(this, m == null ? "Add movie" : "Edit movie",
                new FormDialog.Field("Title", m == null ? "" : m.title()),
                new FormDialog.Field("Genre", m == null ? "" : m.genre()),
                new FormDialog.Field("Language", m == null ? "" : m.language()),
                new FormDialog.Field("Duration (min)", m == null ? "" : m.duration()),
                new FormDialog.Field("Rating", m == null ? "UA" : m.rating()),
                new FormDialog.Field("Release date (yyyy-MM-dd)", m == null || m.releaseDate() == null ? "" : m.releaseDate()),
                new FormDialog.Field("Description", m == null ? "" : m.description()));
            if (v == null) return null;
            if (s(v, "Title").isEmpty()) throw new IllegalArgumentException("Title is required.");
            String rd = s(v, "Release date (yyyy-MM-dd)");
            return new Movie(m == null ? 0 : m.id(), s(v, "Title"), s(v, "Genre"), s(v, "Language"),
                Integer.parseInt(s(v, "Duration (min)")), s(v, "Rating"), rd.isEmpty() ? null : LocalDate.parse(rd), s(v, "Description"));
        }
        @Override protected void add() throws Exception { Movie m = ask(null); if (m != null) MovieDao.insert(m); }
        @Override protected void edit(Object[] row) throws Exception {
            Movie old = cache.stream().filter(x -> x.id() == id(row)).findFirst().orElseThrow();
            Movie m = ask(old);
            if (m != null) MovieDao.update(m);
        }
        @Override protected void delete(Object[] row) throws Exception { if (confirmDelete()) MovieDao.delete(id(row)); }
    }

    // ------------------------------------------------------------ CINEMAS
    public static class CinemasPanel extends CrudPanel {
        private List<Cinema> cache = List.of();
        public CinemasPanel() { super(true, true, true); }

        @Override protected Table load() throws Exception {
            cache = CinemaDao.all();
            List<Object[]> rows = new ArrayList<>();
            for (Cinema c : cache) rows.add(new Object[]{c.id(), c.name(), c.city(), c.address()});
            return new Table(new String[]{"ID", "Name", "City", "Address"}, rows);
        }
        private Map<String, Object> ask(Cinema c) {
            return FormDialog.show(this, c == null ? "Add cinema" : "Edit cinema",
                new FormDialog.Field("Name", c == null ? "" : c.name()),
                new FormDialog.Field("City", c == null ? "" : c.city()),
                new FormDialog.Field("Address", c == null ? "" : c.address()));
        }
        @Override protected void add() throws Exception {
            var v = ask(null);
            if (v != null) CinemaDao.insert(s(v, "Name"), s(v, "City"), s(v, "Address"));
        }
        @Override protected void edit(Object[] row) throws Exception {
            Cinema old = cache.stream().filter(x -> x.id() == id(row)).findFirst().orElseThrow();
            var v = ask(old);
            if (v != null) CinemaDao.update(old.id(), s(v, "Name"), s(v, "City"), s(v, "Address"));
        }
        @Override protected void delete(Object[] row) throws Exception { if (confirmDelete()) CinemaDao.delete(id(row)); }
    }

    // ------------------------------------------------------------ SCREENS
    public static class ScreensPanel extends CrudPanel {
        private List<Screen> cache = List.of();
        public ScreensPanel() { super(true, true, true); }

        @Override protected Table load() throws Exception {
            cache = ScreenDao.all();
            return Db.query("""
                SELECT s.screen_id AS ID, c.name AS Cinema, s.screen_name AS Screen, COUNT(se.seat_id) AS Seats
                FROM screens s JOIN cinemas c ON c.cinema_id = s.cinema_id
                LEFT JOIN seats se ON se.screen_id = s.screen_id
                GROUP BY s.screen_id, c.name, s.screen_name ORDER BY c.name, s.screen_name""");
        }
        private Map<String, Object> ask(Screen sc) throws Exception {
            List<Cinema> cinemas = CinemaDao.all();
            if (cinemas.isEmpty()) throw new IllegalStateException("Add a cinema first.");
            Cinema init = sc == null ? cinemas.get(0) : cinemas.stream().filter(c -> c.id() == sc.cinemaId()).findFirst().orElse(null);
            return FormDialog.show(this, sc == null ? "Add screen" : "Edit screen",
                new FormDialog.Field("Cinema", init, cinemas.toArray()),
                new FormDialog.Field("Screen name", sc == null ? "" : sc.name()));
        }
        @Override protected void add() throws Exception {
            var v = ask(null);
            if (v != null) ScreenDao.insert(((Cinema) v.get("Cinema")).id(), s(v, "Screen name"));
        }
        @Override protected void edit(Object[] row) throws Exception {
            Screen old = cache.stream().filter(x -> x.id() == id(row)).findFirst().orElseThrow();
            var v = ask(old);
            if (v != null) ScreenDao.update(old.id(), ((Cinema) v.get("Cinema")).id(), s(v, "Screen name"));
        }
        @Override protected void delete(Object[] row) throws Exception { if (confirmDelete()) ScreenDao.delete(id(row)); }
    }

    // ------------------------------------------------------------ SEATS
    public static class SeatsPanel extends CrudPanel {
        private final JComboBox<Screen> screenBox = new JComboBox<>();
        private boolean loading;
        public SeatsPanel() {
            super(true, true, true);
            top.add(Util.bold("Screen:"));
            top.add(screenBox);
            screenBox.addActionListener(e -> { if (!loading) refresh(); });
            JButton gen = util.Theme.secondary("Generate seat grid");
            gen.addActionListener(e -> generate());
            buttons.add(gen, 0);
        }

        @Override protected Table load() throws Exception {
            loading = true;
            Screen prev = (Screen) screenBox.getSelectedItem();
            screenBox.removeAllItems();
            for (Screen sc : ScreenDao.all()) screenBox.addItem(sc);
            if (prev != null)
                for (int i = 0; i < screenBox.getItemCount(); i++)
                    if (screenBox.getItemAt(i).id() == prev.id()) screenBox.setSelectedIndex(i);
            loading = false;
            Screen sc = (Screen) screenBox.getSelectedItem();
            List<Object[]> rows = new ArrayList<>();
            if (sc != null)
                for (Seat s : SeatDao.byScreen(sc.id())) rows.add(new Object[]{s.id(), s.row(), s.number(), s.label(), s.type()});
            return new Table(new String[]{"ID", "Row", "Number", "Seat", "Type"}, rows);
        }
        private Screen current() {
            Screen sc = (Screen) screenBox.getSelectedItem();
            if (sc == null) throw new IllegalStateException("Create a screen first.");
            return sc;
        }
        private void generate() {
            try {
                Screen sc = current();
                var v = FormDialog.show(this, "Generate seats for " + sc,
                    new FormDialog.Field("Rows (1-26)", "6"), new FormDialog.Field("Seats per row", "8"),
                    new FormDialog.Field("Premium rows (last N)", "2"));
                if (v == null) return;
                int n = SeatDao.generate(sc.id(), Integer.parseInt(s(v, "Rows (1-26)")),
                    Integer.parseInt(s(v, "Seats per row")), Integer.parseInt(s(v, "Premium rows (last N)")));
                JOptionPane.showMessageDialog(this, n + " seat(s) created (existing seats were kept).");
                refresh();
            } catch (Exception e) { util.Theme.error(this, e); }
        }
        @Override protected void add() throws Exception {
            Screen sc = current();
            var v = FormDialog.show(this, "Add seat", new FormDialog.Field("Row letter", "A"),
                new FormDialog.Field("Seat number", "1"), new FormDialog.Field("Type", "REGULAR", TYPES));
            if (v == null) return;
            String row = s(v, "Row letter");
            if (row.length() != 1 || !Character.isLetter(row.charAt(0))) throw new IllegalArgumentException("Row must be a single letter.");
            SeatDao.add(sc.id(), row, Integer.parseInt(s(v, "Seat number")), s(v, "Type"));
        }
        @Override protected void edit(Object[] row) throws Exception {
            var v = FormDialog.show(this, "Change seat type for " + row[3], new FormDialog.Field("Type", row[4], TYPES));
            if (v != null) SeatDao.updateType(id(row), s(v, "Type"));
        }
        @Override protected void delete(Object[] row) throws Exception { if (confirmDelete()) SeatDao.delete(id(row)); }
    }

    // ------------------------------------------------------------ SHOWS
    public static class ShowsPanel extends CrudPanel {
        private List<Show> cache = List.of();
        public ShowsPanel() { super(true, true, true); }

        @Override protected Table load() throws Exception {
            cache = ShowDao.all();
            List<Object[]> rows = new ArrayList<>();
            for (Show s : cache) rows.add(new Object[]{s.id(), s.movie(), s.cinema(), s.screen(), s.date(), s.time(), s.price()});
            return new Table(new String[]{"ID", "Movie", "Cinema", "Screen", "Date", "Time", "Base price"}, rows);
        }
        private Show ask(Show old) throws Exception {
            List<Movie> movies = MovieDao.all();
            List<Screen> screens = ScreenDao.all();
            if (movies.isEmpty() || screens.isEmpty()) throw new IllegalStateException("Add at least one movie and one screen first.");
            Movie im = old == null ? movies.get(0) : movies.stream().filter(m -> m.id() == old.movieId()).findFirst().orElse(null);
            Screen is = old == null ? screens.get(0) : screens.stream().filter(x -> x.id() == old.screenId()).findFirst().orElse(null);
            var v = FormDialog.show(this, old == null ? "Add show" : "Edit show",
                new FormDialog.Field("Movie", im, movies.toArray()),
                new FormDialog.Field("Screen", is, screens.toArray()),
                new FormDialog.Field("Date (yyyy-MM-dd)", old == null ? LocalDate.now().plusDays(1) : old.date()),
                new FormDialog.Field("Time (HH:mm)", old == null ? "18:00" : old.time()),
                new FormDialog.Field("Base price", old == null ? "250" : old.price()));
            if (v == null) return null;
            BigDecimal price = new BigDecimal(s(v, "Base price"));
            if (price.signum() <= 0) throw new IllegalArgumentException("Price must be positive.");
            return new Show(old == null ? 0 : old.id(), ((Movie) v.get("Movie")).id(), ((Screen) v.get("Screen")).id(), 0,
                LocalDate.parse(s(v, "Date (yyyy-MM-dd)")), LocalTime.parse(s(v, "Time (HH:mm)")), price, "", "", "");
        }
        @Override protected void add() throws Exception {
            Show s = ask(null);
            if (s != null) ShowDao.insert(s.movieId(), s.screenId(), s.date(), s.time(), s.price());
        }
        @Override protected void edit(Object[] row) throws Exception {
            Show old = cache.stream().filter(x -> x.id() == id(row)).findFirst().orElseThrow();
            Show s = ask(old);
            if (s != null) ShowDao.update(old.id(), s.movieId(), s.screenId(), s.date(), s.time(), s.price());
        }
        @Override protected void delete(Object[] row) throws Exception { if (confirmDelete()) ShowDao.delete(id(row)); }
    }

    // ------------------------------------------------------------ READ-ONLY
    public static class UsersPanel extends CrudPanel {
        public UsersPanel() { super(false, false, false); }
        @Override protected Table load() throws Exception { return UserDao.all(); }
    }

    public static class BookingsPanel extends CrudPanel {
        public BookingsPanel() { super(false, false, false); }
        @Override protected Table load() throws Exception { return BookingDao.all(); }
    }

    private static final class Util {
        static JLabel bold(String t) { return util.Theme.label(t, util.Theme.BOLD, util.Theme.TEXT); }
    }
}
