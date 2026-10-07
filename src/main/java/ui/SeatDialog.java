package ui;

import dao.SeatDao;
import model.*;
import service.BookingException;
import service.BookingService;
import util.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.*;

/** Seat map + booking confirmation for one show. */
public class SeatDialog extends JDialog {
    private final User user;
    private final Show show;
    private final JPanel grid = new JPanel();
    private final List<SeatButton> buttons = new ArrayList<>();
    private final JLabel summary = Theme.label("", Theme.BOLD, Theme.TEXT);
    private final JButton pay = Theme.primary("Proceed to Payment");

    public SeatDialog(Window owner, User user, Show show) {
        super(owner, "Select seats", ModalityType.APPLICATION_MODAL);
        this.user = user;
        this.show = show;
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        root.setBackground(Color.WHITE);

        JPanel top = new JPanel(new GridLayout(2, 1));
        top.setOpaque(false);
        top.add(Theme.label(show.movie(), Theme.H2, Theme.TEXT));
        top.add(Theme.label(show.cinema() + " | " + show.screen() + " | " + show.date() + " " + show.time()
            + " | Regular " + Theme.money(show.price()) + ", Premium " + Theme.money(BookingService.seatPrice(show.price(), "PREMIUM")),
            Theme.BASE, Theme.MUTED));
        root.add(top, BorderLayout.NORTH);

        grid.setLayout(new BoxLayout(grid, BoxLayout.Y_AXIS));
        grid.setBackground(Color.WHITE);
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        JLabel screen = Theme.label("S C R E E N", Theme.SMALL, Theme.MUTED);
        screen.setHorizontalAlignment(SwingConstants.CENTER);
        screen.setOpaque(true);
        screen.setBackground(new Color(0xE2E8F0));
        screen.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        center.add(screen, BorderLayout.NORTH);
        center.add(new JScrollPane(grid) {{ setBorder(null); }}, BorderLayout.CENTER);
        center.add(legend(), BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JButton cancel = Theme.secondary("Cancel");
        cancel.addActionListener(e -> dispose());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(cancel); btns.add(pay);
        bottom.add(summary, BorderLayout.CENTER);
        bottom.add(btns, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);
        pay.addActionListener(e -> checkout());

        setContentPane(root);
        loadSeats();
        setSize(Math.max(620, getPreferredSize().width), 560);
        setLocationRelativeTo(owner);
    }

    private JPanel legend() {
        JPanel l = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 4));
        l.setOpaque(false);
        l.add(chip(Color.WHITE, "Regular"));
        l.add(chip(new Color(0xFEF3C7), "Premium"));
        l.add(chip(Theme.ACCENT, "Selected"));
        l.add(chip(new Color(0xCBD5E1), "Booked"));
        return l;
    }

    private JLabel chip(Color c, String t) {
        JLabel l = new JLabel(t, new Icon() {
            public void paintIcon(Component comp, Graphics g, int x, int y) {
                g.setColor(c); g.fillRoundRect(x, y, 16, 16, 5, 5);
                g.setColor(new Color(0x94A3B8)); g.drawRoundRect(x, y, 16, 16, 5, 5);
            }
            public int getIconWidth() { return 17; }
            public int getIconHeight() { return 17; }
        }, SwingConstants.LEFT);
        l.setFont(Theme.SMALL);
        return l;
    }

    private void loadSeats() {
        buttons.clear();
        grid.removeAll();
        try {
            List<Seat> seats = SeatDao.byScreen(show.screenId());
            Set<Integer> taken = SeatDao.booked(show.id());
            Map<String, List<Seat>> rows = new TreeMap<>();
            for (Seat s : seats) rows.computeIfAbsent(s.row(), k -> new ArrayList<>()).add(s);
            for (var e : rows.entrySet()) {
                JPanel r = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 3));
                r.setBackground(Color.WHITE);
                JLabel rl = Theme.label(e.getKey(), Theme.BOLD, Theme.MUTED);
                rl.setPreferredSize(new Dimension(20, 30));
                r.add(rl);
                for (Seat s : e.getValue()) {
                    SeatButton b = new SeatButton(s, taken.contains(s.id()), this::update);
                    buttons.add(b);
                    r.add(b);
                }
                grid.add(r);
            }
        } catch (SQLException ex) { Theme.error(this, ex); }
        grid.revalidate();
        grid.repaint();
        update();
    }

    private List<SeatButton> selected() {
        return buttons.stream().filter(b -> b.selected).toList();
    }

    private BigDecimal total() {
        BigDecimal t = BigDecimal.ZERO;
        for (SeatButton b : selected()) t = t.add(BookingService.seatPrice(show.price(), b.seat.type()));
        return t;
    }

    private void update() {
        List<SeatButton> sel = selected();
        if (sel.isEmpty()) summary.setText("No seats selected");
        else {
            StringJoiner j = new StringJoiner(", ");
            sel.forEach(b -> j.add(b.seat.label()));
            summary.setText(sel.size() + " seat(s): " + j + "   |   Total: " + Theme.money(total()));
        }
        pay.setEnabled(!sel.isEmpty());
    }

    private void checkout() {
        BigDecimal total = total();
        List<Integer> ids = selected().stream().map(b -> b.seat.id()).toList();
        PaymentDialog.Choice c = PaymentDialog.ask(this, total);
        if (c == null) return;
        try {
            int id = BookingService.book(user.id(), show.id(), ids, c.method(), c.simulateFailure());
            JOptionPane.showMessageDialog(this,
                "Booking confirmed!\n\nBooking ID: " + id + "\nMovie: " + show.movie() + "\n" + show.cinema() + " - " + show.screen()
                    + "\n" + show.date() + " at " + show.time() + "\nSeats: " + summaryOf() + "\nPaid: " + Theme.money(total) + " via " + c.method(),
                "Booking Confirmation", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (BookingException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Booking failed", JOptionPane.WARNING_MESSAGE);
            loadSeats();                                   // refresh: seats may have been taken by someone else
        }
    }

    private String summaryOf() {
        StringJoiner j = new StringJoiner(", ");
        selected().forEach(b -> j.add(b.seat.label()));
        return j.toString();
    }

    /** Custom-painted seat so the colours look the same in every Look & Feel. */
    static class SeatButton extends JButton {
        final Seat seat;
        final boolean taken;
        boolean selected;

        SeatButton(Seat seat, boolean taken, Runnable onChange) {
            this.seat = seat;
            this.taken = taken;
            setText(String.valueOf(seat.number()));
            setPreferredSize(new Dimension(40, 32));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setEnabled(!taken);
            setFont(Theme.SMALL);
            setToolTipText(seat.label() + " (" + seat.type().toLowerCase() + ")" + (taken ? " - booked" : ""));
            if (!taken) setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> { selected = !selected; repaint(); onChange.run(); });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = taken ? new Color(0xCBD5E1) : selected ? Theme.ACCENT : seat.isPremium() ? new Color(0xFEF3C7) : Color.WHITE;
            g.setColor(fill);
            g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
            g.setColor(selected ? Theme.ACCENT : new Color(0x94A3B8));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
            g.setFont(getFont());
            g.setColor(taken ? new Color(0x64748B) : selected ? Color.WHITE : Theme.TEXT);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }
}
