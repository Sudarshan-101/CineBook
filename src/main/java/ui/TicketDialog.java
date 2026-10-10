package ui;

import model.Show;
import util.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Confirmation "ticket" shown after a successful booking. */
public final class TicketDialog {
    private TicketDialog() {}

    public static void show(Component parent, int bookingId, Show show, String seats, BigDecimal paid, String method) {
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(parent), "Booking confirmed", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(SeatDialog.BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel ok = Theme.label("Booking confirmed!", Theme.H2.deriveFont(22f), new Color(0x4ADE80));
        ok.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(ok, BorderLayout.NORTH);

        JPanel ticket = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight(), notchY = h - 78;
                Area a = new Area(new RoundRectangle2D.Double(0, 0, w, h, 24, 24));
                a.subtract(new Area(new Ellipse2D.Double(-12, notchY - 12, 24, 24)));
                a.subtract(new Area(new Ellipse2D.Double(w - 12, notchY - 12, 24, 24)));
                g.setPaint(new GradientPaint(0, 0, new Color(0x1E293B), w, h, new Color(0x0F172A)));
                g.fill(a);
                g.setColor(new Color(0x334155));
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 6f}, 0f));
                g.drawLine(16, notchY, w - 16, notchY);
                g.dispose();
            }
        };
        ticket.setOpaque(false);
        ticket.setLayout(new BoxLayout(ticket, BoxLayout.Y_AXIS));
        ticket.setBorder(BorderFactory.createEmptyBorder(22, 26, 18, 26));
        ticket.add(line(show.movie(), Theme.H2.deriveFont(24f), Color.WHITE));
        ticket.add(Box.createVerticalStrut(4));
        ticket.add(line(show.cinema() + "  |  " + show.screen(), Theme.BASE, SeatDialog.SOFT));
        ticket.add(Box.createVerticalStrut(16));
        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 12));
        grid.setOpaque(false);
        grid.add(field("DATE", show.date().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH))));
        grid.add(field("TIME", show.time().format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))));
        grid.add(field("SEATS", seats));
        grid.add(field("BOOKING ID", "#" + bookingId));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        ticket.add(grid);
        ticket.add(Box.createVerticalGlue());
        ticket.add(Box.createVerticalStrut(34));
        JPanel pay = new JPanel(new BorderLayout());
        pay.setOpaque(false);
        pay.add(Theme.label("Paid via " + method.replace('_', ' '), Theme.BASE, SeatDialog.SOFT), BorderLayout.WEST);
        pay.add(Theme.label(Theme.money(paid), Theme.H2, Color.WHITE), BorderLayout.EAST);
        pay.setAlignmentX(Component.LEFT_ALIGNMENT);
        ticket.add(pay);
        root.add(ticket, BorderLayout.CENTER);

        JButton done = Theme.primary("Done");
        done.addActionListener(e -> d.dispose());
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.CENTER));
        foot.setOpaque(false);
        foot.add(done);
        root.add(foot, BorderLayout.SOUTH);

        d.setContentPane(root);
        d.setSize(460, 460);
        d.setLocationRelativeTo(parent);
        d.getRootPane().setDefaultButton(done);
        d.setVisible(true);
    }

    private static JLabel line(String t, Font f, Color c) {
        JLabel l = Theme.label(t, f, c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JPanel field(String k, String v) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        p.add(Theme.label(k, Theme.SMALL.deriveFont(Font.BOLD, 11f), SeatDialog.SOFT));
        p.add(Theme.label(v, Theme.BOLD.deriveFont(15f), Color.WHITE));
        return p;
    }
}
