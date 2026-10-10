package ui;

import model.User;
import util.Theme;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AdminFrame extends JFrame {
    public AdminFrame(User u) {
        super("CineBook Admin - " + u.name());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(Theme.header("Admin: " + u.name(), () -> { dispose(); new LoginFrame().setVisible(true); }), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.BOLD);
        ReportsPanel reports = new ReportsPanel();
        tabs.addTab("Dashboard & Reports", reports);
        tabs.addTab("Movies", new AdminPanels.MoviesPanel());
        tabs.addTab("Cinemas", new AdminPanels.CinemasPanel());
        tabs.addTab("Screens", new AdminPanels.ScreensPanel());
        tabs.addTab("Seats", new AdminPanels.SeatsPanel());
        tabs.addTab("Shows", new AdminPanels.ShowsPanel());
        tabs.addTab("Users", new AdminPanels.UsersPanel());
        tabs.addTab("Bookings", new AdminPanels.BookingsPanel());
        // refresh data whenever a tab is opened
        tabs.addChangeListener(e -> refreshTab(tabs.getSelectedComponent()));
        add(tabs, BorderLayout.CENTER);
        refreshTab(reports);
    }

    private static void refreshTab(Component c) {
        if (c instanceof CrudPanel p) p.refresh();
        else if (c instanceof ReportsPanel r) r.refresh();
    }
}
