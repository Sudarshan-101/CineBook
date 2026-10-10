package ui;

import dao.MovieDao;
import dao.ShowDao;
import model.*;
import util.Poster;
import util.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/** Customer: browse movie posters -> pick a date -> pick a showtime -> seat map. */
public class BrowsePanel extends JPanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final User user;
    private List<Movie> movies = List.of();
    private Movie selected;
    private LocalDate selectedDate;
    private List<Show> shows = List.of();
    private Map<Integer, int[]> avail = Map.of();

    private final JTextField search = new JTextField(22);
    private final JComboBox<String> genreBox = new JComboBox<>();
    private final JComboBox<String> cityBox = new JComboBox<>();
    private final WrapPanel grid = new WrapPanel(14, 14);
    private final JPanel detail = new ScrollColumn();
    private boolean loading;

    public BrowsePanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(Theme.BG);

        // ---- top bar: search + genre + city
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        JButton clear = Theme.secondary("Clear");
        search.putClientProperty("JTextField.placeholderText", "Search title, genre or language...");
        bar.add(Theme.label("Find a movie", Theme.BOLD, Theme.TEXT));
        bar.add(search);
        bar.add(Theme.label("Genre", Theme.BASE, Theme.MUTED)); bar.add(genreBox);
        bar.add(Theme.label("City", Theme.BASE, Theme.MUTED)); bar.add(cityBox);
        bar.add(clear);
        add(bar, BorderLayout.NORTH);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { loadMovies(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { loadMovies(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { loadMovies(); }
        });
        genreBox.addActionListener(e -> { if (!loading) loadMovies(); });
        cityBox.addActionListener(e -> { if (!loading) renderShows(); });
        clear.addActionListener(e -> { search.setText(""); genreBox.setSelectedIndex(0); });

        // ---- left: poster grid
        grid.setBackground(Color.WHITE);
        grid.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JPanel left = Theme.card(new BorderLayout(0, 8));
        left.add(Theme.label("Now showing", Theme.H2, Theme.TEXT), BorderLayout.NORTH);
        JScrollPane gs = Theme.scroll(grid);
        gs.getVerticalScrollBar().setUnitIncrement(24);
        gs.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        left.add(gs, BorderLayout.CENTER);

        // ---- right: details + showtimes
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        detail.setBackground(Color.WHITE);
        detail.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 10));
        JPanel right = Theme.card(new BorderLayout());
        JScrollPane ds = Theme.scroll(detail);
        ds.setBorder(null);
        ds.getVerticalScrollBar().setUnitIncrement(24);
        ds.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        right.add(ds, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.5);
        split.setBorder(null);
        split.setDividerSize(10);
        add(split, BorderLayout.CENTER);

        loading = true;
        try {
            genreBox.addItem("All");
            for (String g : MovieDao.genres()) genreBox.addItem(g);
            cityBox.addItem("All cities");
            for (String c : util.Db.list("SELECT DISTINCT city FROM cinemas ORDER BY city", rs -> rs.getString(1))) cityBox.addItem(c);
        } catch (Exception ex) { Theme.error(this, ex); }
        loading = false;
        loadMovies();
    }

    // ------------------------------------------------------------------ movies
    private void loadMovies() {
        try {
            String g = (String) genreBox.getSelectedItem();
            movies = MovieDao.search(search.getText().trim()).stream()
                .filter(m -> g == null || g.equals("All") || g.equalsIgnoreCase(m.genre())).toList();
            grid.removeAll();
            for (Movie m : movies) grid.add(new MovieCard(m));
            if (movies.isEmpty()) grid.add(Theme.label("No movies match your search.", Theme.BASE, Theme.MUTED));
            grid.revalidate(); grid.repaint();
            Movie keep = selected == null ? null : movies.stream().filter(m -> m.id() == selected.id()).findFirst().orElse(null);
            selectMovie(keep != null ? keep : movies.isEmpty() ? null : movies.get(0));
        } catch (Exception ex) { Theme.error(this, ex); }
    }

    private void selectMovie(Movie m) {
        selected = m;
        selectedDate = null;
        for (Component c : grid.getComponents()) if (c instanceof MovieCard mc) mc.repaint();
        try {
            if (m == null) { shows = List.of(); avail = Map.of(); }
            else { shows = ShowDao.upcomingForMovie(m.id()); avail = ShowDao.availability(m.id()); }
        } catch (Exception ex) { Theme.error(this, ex); shows = List.of(); }
        renderDetail();
    }

    // ------------------------------------------------------------------ detail
    private void renderDetail() {
        detail.removeAll();
        if (selected == null) {
            detail.add(pad(Theme.label("Select a movie to see showtimes", Theme.H2, Theme.MUTED)));
            refresh();
            return;
        }
        Movie m = selected;
        // banner
        JPanel banner = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Poster.paint(g, m.title(), m.genre(), 0, 0, getWidth(), getHeight(), 18, 26f);
            }
        };
        banner.setPreferredSize(new Dimension(100, 110));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        banner.setAlignmentX(LEFT_ALIGNMENT);
        detail.add(banner);
        detail.add(Box.createVerticalStrut(12));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.setOpaque(false);
        chips.add(chip(m.rating() == null ? "NR" : m.rating(), Theme.ACCENT));
        chips.add(chip(m.genre(), new Color(0x475569)));
        chips.add(chip(m.language(), new Color(0x475569)));
        chips.add(chip(m.duration() / 60 + "h " + m.duration() % 60 + "m", new Color(0x475569)));
        if (m.releaseDate() != null) chips.add(chip(String.valueOf(m.releaseDate().getYear()), new Color(0x475569)));
        detail.add(left(chips));

        JTextArea desc = new JTextArea(m.description() == null ? "" : m.description());
        desc.setEditable(false); desc.setLineWrap(true); desc.setWrapStyleWord(true);
        desc.setOpaque(false); desc.setFont(Theme.BASE); desc.setForeground(Theme.MUTED);
        desc.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
        detail.add(left(desc));

        detail.add(left(Theme.label("Choose a date", Theme.BOLD, Theme.TEXT)));
        detail.add(Box.createVerticalStrut(6));

        SortedMap<LocalDate, List<Show>> byDate = shows.stream()
            .filter(s -> !s.date().isAfter(LocalDate.now().plusDays(6)))
            .collect(Collectors.groupingBy(Show::date, TreeMap::new, Collectors.toList()));
        if (byDate.isEmpty()) {
            detail.add(left(Theme.label("No upcoming shows for this movie yet.", Theme.BASE, Theme.MUTED)));
        } else {
            if (selectedDate == null || !byDate.containsKey(selectedDate)) selectedDate = byDate.firstKey();
            JPanel dates = new WrapPanel(8, 6);
            dates.setOpaque(false);
            for (LocalDate d : byDate.keySet()) dates.add(new DateChip(d));
            detail.add(left(dates));
            detail.add(Box.createVerticalStrut(10));
            detail.add(left(Theme.label("Showtimes", Theme.BOLD, Theme.TEXT)));
            detail.add(Box.createVerticalStrut(4));
            showsPanel.removeAll();
            showsPanel.setLayout(new BoxLayout(showsPanel, BoxLayout.Y_AXIS));
            showsPanel.setOpaque(false);
            detail.add(left(showsPanel));
            renderShows();
        }
        detail.add(Box.createVerticalGlue());
        refresh();
    }

    private final JPanel showsPanel = new JPanel();

    private void renderShows() {
        showsPanel.removeAll();
        if (selected != null && selectedDate != null) {
            String city = (String) cityBox.getSelectedItem();
            Map<String, List<Show>> byCinema = shows.stream()
                .filter(s -> s.date().equals(selectedDate))
                .filter(s -> city == null || city.equals("All cities") || cinemaCity.getOrDefault(s.cinemaId(), city).equals(city))
                .collect(Collectors.groupingBy(s -> s.cinema() + "|" + s.cinemaId(), TreeMap::new, Collectors.toList()));
            if (byCinema.isEmpty()) {
                showsPanel.add(left(Theme.label("No shows on this date" + (city == null || city.equals("All cities") ? "" : " in " + city) + ".", Theme.BASE, Theme.MUTED)));
            }
            for (var e : byCinema.entrySet()) {
                String name = e.getKey().substring(0, e.getKey().lastIndexOf('|'));
                JPanel row = new JPanel(new BorderLayout(0, 6));
                row.setOpaque(false);
                row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER), BorderFactory.createEmptyBorder(6, 0, 10, 0)));
                row.add(Theme.label(name, Theme.BOLD, Theme.TEXT), BorderLayout.NORTH);
                JPanel times = new WrapPanel(8, 6);
                times.setOpaque(false);
                e.getValue().stream().sorted(Comparator.comparing(Show::time)).forEach(s -> times.add(new ShowChip(s)));
                row.add(times, BorderLayout.CENTER);
                showsPanel.add(left(row));
            }
        }
        refresh();
    }

    private final Map<Integer, String> cinemaCity = loadCities();
    private static Map<Integer, String> loadCities() {
        Map<Integer, String> m = new HashMap<>();
        try { for (String[] r : util.Db.list("SELECT cinema_id, city FROM cinemas", rs -> new String[]{rs.getString(1), rs.getString(2)})) m.put(Integer.parseInt(r[0]), r[1]); }
        catch (Exception ignored) {}
        return m;
    }

    private void refresh() { detail.revalidate(); detail.repaint(); }

    private void openSeats(Show s) {
        new SeatDialog(SwingUtilities.getWindowAncestor(this), user, s).setVisible(true);
        // seats may have been booked: reload availability
        if (selected != null) {
            try { avail = ShowDao.availability(selected.id()); shows = ShowDao.upcomingForMovie(selected.id()); } catch (Exception ignored) {}
            renderShows();
        }
    }

    // ------------------------------------------------------------------ helpers
    private static JComponent left(JComponent c) { c.setAlignmentX(LEFT_ALIGNMENT); return c; }
    private static JComponent pad(JComponent c) { c.setBorder(BorderFactory.createEmptyBorder(20, 4, 20, 4)); return left(c); }

    private static JLabel chip(String text, Color bg) {
        JLabel l = new JLabel(text == null ? "-" : text) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(getBackground());
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g.dispose();
                super.paintComponent(g0);
            }
        };
        l.setOpaque(false); l.setBackground(bg); l.setForeground(Color.WHITE);
        l.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        l.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        return l;
    }

    // ------------------------------------------------------------------ components
    private class MovieCard extends JPanel {
        final Movie m;
        boolean hover;
        MovieCard(Movie m) {
            this.m = m;
            setPreferredSize(new Dimension(150, 228));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(m.title() + " - " + m.genre() + " | " + m.language());
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { selectMovie(m); }
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean sel = selected != null && selected.id() == m.id();
            int w = getWidth(), h = getHeight();
            if (sel) { g.setColor(Theme.ACCENT); g.fillRoundRect(0, 0, w, h, 18, 18); }
            else if (hover) { g.setColor(new Color(0xCBD5E1)); g.fillRoundRect(0, 0, w, h, 18, 18); }
            Poster.paint(g, m.title(), m.genre(), 3, 3, w - 6, h - 6, 15, 15f);
            // rating badge
            if (m.rating() != null) {
                g.setFont(Theme.SMALL.deriveFont(Font.BOLD, 11f));
                int bw = g.getFontMetrics().stringWidth(m.rating()) + 14;
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRoundRect(w - bw - 10, 10, bw, 20, 20, 20);
                g.setColor(Color.WHITE);
                g.drawString(m.rating(), w - bw - 3, 24);
            }
            g.dispose();
        }
    }

    /** Column that follows the scroll pane's width (so nothing is cut off on the right). */
    private static class ScrollColumn extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** Base for our flat, custom-painted buttons (look identical in every Look & Feel). */
    private abstract static class Pill extends JComponent {
        boolean hover;
        Runnable action;
        Pill(Dimension size) {
            setPreferredSize(size); setMinimumSize(size); setMaximumSize(size);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { if (isEnabled() && action != null) action.run(); }
            });
        }
        static void center(Graphics2D g, String t, int w, int y) { g.drawString(t, (w - g.getFontMetrics().stringWidth(t)) / 2, y); }
        Graphics2D g2(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            return g;
        }
    }

    private class DateChip extends Pill {
        final LocalDate d;
        DateChip(LocalDate d) {
            super(new Dimension(58, 64));
            this.d = d;
            setToolTipText(d.toString());
            action = () -> { selectedDate = d; renderDetail(); };
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = g2(g0);
            boolean on = d.equals(selectedDate);
            int w = getWidth(), h = getHeight();
            g.setColor(on ? Theme.ACCENT : hover ? new Color(0xFFF1F2) : Color.WHITE);
            g.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
            g.setColor(on ? Theme.ACCENT : hover ? Theme.ACCENT : new Color(0xCBD5E1));
            g.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
            Color main = on ? Color.WHITE : Theme.TEXT, soft = on ? new Color(0xFFE4E6) : Theme.MUTED;
            g.setFont(Theme.SMALL.deriveFont(Font.BOLD, 10f)); g.setColor(soft);
            center(g, d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(), w, 16);
            g.setFont(Theme.BOLD.deriveFont(Font.BOLD, 20f)); g.setColor(main);
            center(g, String.valueOf(d.getDayOfMonth()), w, 39);
            g.setFont(Theme.SMALL.deriveFont(Font.PLAIN, 10f)); g.setColor(soft);
            center(g, d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), w, 55);
            g.dispose();
        }
    }

    private class ShowChip extends Pill {
        final String time, sub;
        final Color col;
        final boolean soldOut;
        ShowChip(Show s) {
            super(new Dimension(128, 50));
            int[] a = avail.getOrDefault(s.id(), new int[]{0, 0});
            int left = Math.max(0, a[1] - a[0]);
            soldOut = a[1] > 0 && left == 0;
            double ratio = a[1] == 0 ? 1 : (double) left / a[1];
            col = soldOut ? new Color(0x94A3B8) : ratio < 0.3 ? new Color(0xD97706) : new Color(0x16A34A);
            time = TIME.format(s.time()).toUpperCase();
            sub = Theme.money(s.price()).replace(".00", "") + "  |  " + (soldOut ? "Sold out" : left + " left");
            setToolTipText(s.screen() + (soldOut ? " - sold out" : " - " + left + " of " + a[1] + " seats available"));
            setEnabled(!soldOut);
            if (soldOut) setCursor(Cursor.getDefaultCursor());
            action = () -> openSeats(s);
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = g2(g0);
            int w = getWidth(), h = getHeight();
            g.setColor(hover && !soldOut ? new Color(col.getRed(), col.getGreen(), col.getBlue(), 28) : Color.WHITE);
            g.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
            g.setColor(col); g.setStroke(new BasicStroke(hover && !soldOut ? 2f : 1.2f));
            g.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
            g.setFont(Theme.BOLD.deriveFont(Font.BOLD, 14f)); g.setColor(col);
            center(g, time, w, 21);
            g.setFont(Theme.SMALL.deriveFont(Font.PLAIN, 11f)); g.setColor(soldOut ? col : Theme.MUTED);
            center(g, sub, w, 38);
            g.dispose();
        }
    }
}
