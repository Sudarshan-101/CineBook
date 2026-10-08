package ui;

import model.User;
import util.Theme;
import javax.swing.*;
import java.awt.*;

public class CustomerFrame extends JFrame {
    public CustomerFrame(User u) {
        super("CineBook - " + u.name());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1150, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(Theme.header("Welcome, " + u.name(), () -> { dispose(); new LoginFrame().setVisible(true); }), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.BOLD);
        MyBookingsPanel mine = new MyBookingsPanel(u);
        tabs.addTab("Browse & Book", new BrowsePanel(u));
        tabs.addTab("My Bookings", mine);
        tabs.addChangeListener(e -> { if (tabs.getSelectedComponent() == mine) mine.refresh(); });
        add(tabs, BorderLayout.CENTER);
    }
}
