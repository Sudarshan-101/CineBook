package ui;

import dao.CinemaDao;
import dao.MovieDao;
import dao.ShowDao;
import model.*;
import util.Theme;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/** Customer: search movies -> choose cinema/show -> open seat map. */
public class BrowsePanel extends JPanel {
    private final User user;
    private List<Movie> movies = List.of();
    private List<Show> shows = List.of();
    private final DefaultTableModel mm = Theme.model(), sm = Theme.model();
    private final JTable mt = new JTable(mm), st = new JTable(sm);
    private final JTextField search = new JTextField(24);
    private final JComboBox<Object> cinemaBox = new JComboBox<>();
    private final JLabel title = Theme.label("Select a movie", Theme.H2, Theme.TEXT);
    private final JLabel meta = Theme.label(" ", Theme.BASE, Theme.MUTED);
    private final JTextArea desc = new JTextArea(3, 20);

    public BrowsePanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(Theme.BG);
        Theme.table(mt); Theme.table(st);
        mt.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        st.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // search bar
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        JButton go = Theme.primary("Search");
        JButton clear = Theme.secondary("Clear");
        bar.add(Theme.label("Search movies (title, genre, language):", Theme.BOLD, Theme.TEXT));
        bar.add(search); bar.add(go); bar.add(clear);
        go.addActionListener(e -> loadMovies());
        search.addActionListener(e -> loadMovies());
        clear.addActionListener(e -> { search.setText(""); loadMovies(); });
        add(bar, BorderLayout.NORTH);

        // left: movies
        JPanel left = Theme.card(new BorderLayout(0, 8));
        left.add(Theme.label("Now showing", Theme.H2, Theme.TEXT), BorderLayout.NORTH);
        left.add(Theme.scroll(mt), BorderLayout.CENTER);

        // right: details + shows
        JPanel right = Theme.card(new BorderLayout(0, 8));
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        desc.setEditable(false); desc.setLineWrap(true); desc.setWrapStyleWord(true);
        desc.setOpaque(false); desc.setFont(Theme.BASE); desc.setForeground(Theme.MUTED);
        desc.setAlignmentX(LEFT_ALIGNMENT); title.setAlignmentX(LEFT_ALIGNMENT); meta.setAlignmentX(LEFT_ALIGNMENT);
        head.add(title); head.add(meta); head.add(Box.createVerticalStrut(6)); head.add(desc);
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filter.setOpaque(false);
        filter.add(Theme.label("Cinema:", Theme.BOLD, Theme.TEXT)); filter.add(cinemaBox);
        filter.setAlignmentX(LEFT_ALIGNMENT);
        head.add(filter);
        right.add(head, BorderLayout.NORTH);
        right.add(Theme.scroll(st), BorderLayout.CENTER);
        JButton select = Theme.primary("Select Seats  >");
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        foot.setOpaque(false);
        foot.add(select);
        right.add(foot, BorderLayout.SOUTH);
        select.addActionListener(e -> openSeats());
        st.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) openSeats(); }
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.45);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        try {
            cinemaBox.addItem("All cinemas");
            for (Cinema c : CinemaDao.all()) cinemaBox.addItem(c);
        } catch (Exception ex) { Theme.error(this, ex); }
        cinemaBox.addActionListener(e -> loadShows());
        mt.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) loadShows(); });
        loadMovies();
    }

    private void loadMovies() {
        try {
            movies = MovieDao.search(search.getText().trim());
            mm.setColumnIdentifiers(new String[]{"Title", "Genre", "Language", "Duration", "Rating"});
            mm.setRowCount(0);
            for (Movie m : movies) mm.addRow(new Object[]{m.title(), m.genre(), m.language(), m.duration() + " min", m.rating()});
            if (!movies.isEmpty()) mt.setRowSelectionInterval(0, 0); else loadShows();
        } catch (Exception ex) { Theme.error(this, ex); }
    }

    private void loadShows() {
        int r = mt.getSelectedRow();
        sm.setColumnIdentifiers(new String[]{"Cinema", "Screen", "Date", "Time", "Price"});
        sm.setRowCount(0);
        if (r < 0 || r >= movies.size()) {
            shows = List.of(); title.setText("No movie selected"); meta.setText(" "); desc.setText("");
            return;
        }
        Movie m = movies.get(r);
        title.setText(m.title());
        meta.setText(m.genre() + " | " + m.language() + " | " + m.duration() + " min | " + m.rating());
        desc.setText(m.description() == null ? "" : m.description());
        try {
            Object sel = cinemaBox.getSelectedItem();
            shows = ShowDao.upcomingForMovie(m.id()).stream()
                .filter(s -> !(sel instanceof Cinema c) || c.id() == s.cinemaId()).toList();
            for (Show s : shows) sm.addRow(new Object[]{s.cinema(), s.screen(), s.date(), s.time(), Theme.money(s.price())});
        } catch (Exception ex) { Theme.error(this, ex); }
    }

    private void openSeats() {
        int r = st.getSelectedRow();
        if (r < 0 || r >= shows.size()) {
            JOptionPane.showMessageDialog(this, "Please select a show first.");
            return;
        }
        new SeatDialog(SwingUtilities.getWindowAncestor(this), user, shows.get(r)).setVisible(true);
        loadShows();
    }
}
