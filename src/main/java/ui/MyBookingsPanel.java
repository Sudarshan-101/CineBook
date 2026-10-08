package ui;

import dao.BookingDao;
import model.*;
import service.BookingException;
import service.BookingService;
import util.Theme;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MyBookingsPanel extends JPanel {
    private final User user;
    private List<Booking> list = List.of();
    private final DefaultTableModel model = Theme.model();
    private final JTable table = new JTable(model);

    public MyBookingsPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(Theme.BG);
        Theme.table(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel card = Theme.card(new BorderLayout(0, 10));
        card.add(Theme.label("My bookings", Theme.H2, Theme.TEXT), BorderLayout.NORTH);
        card.add(Theme.scroll(table), BorderLayout.CENTER);
        JButton cancel = Theme.primary("Cancel selected booking");
        JButton refresh = Theme.secondary("Refresh");
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        foot.setOpaque(false);
        foot.add(refresh); foot.add(cancel);
        card.add(foot, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
        refresh.addActionListener(e -> refresh());
        cancel.addActionListener(e -> cancelSelected());
        refresh();
    }

    public void refresh() {
        try {
            list = BookingDao.forUser(user.id());
            model.setColumnIdentifiers(new String[]{"ID", "Movie", "Cinema", "Screen", "Date", "Time", "Seats", "Total", "Status"});
            model.setRowCount(0);
            for (Booking b : list)
                model.addRow(new Object[]{b.id(), b.movie(), b.cinema(), b.screen(), b.date(), b.time(), b.seats(), Theme.money(b.total()), b.status()});
        } catch (Exception ex) { Theme.error(this, ex); }
    }

    private void cancelSelected() {
        int r = table.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Select a booking first."); return; }
        Booking b = list.get(r);
        if (JOptionPane.showConfirmDialog(this, "Cancel booking #" + b.id() + " (" + b.movie() + ")?\nThe amount will be refunded.",
                "Confirm cancellation", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            BookingService.cancel(b.id(), user.id());
            JOptionPane.showMessageDialog(this, "Booking cancelled. " + Theme.money(b.total()) + " refunded.");
        } catch (BookingException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot cancel", JOptionPane.WARNING_MESSAGE);
        }
        refresh();
    }
}
