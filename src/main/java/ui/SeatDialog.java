package ui;

import dao.SeatDao;
import model.*;
import service.BookingException;
import service.BookingService;
import util.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/** Dark "cinema hall" seat picker for one show. Booked seats refresh automatically every few seconds. */
public class SeatDialog extends JDialog {
    static final Color BG = new Color(0x0B1020), PANEL = new Color(0x131A2E), SOFT = new Color(0x94A3B8);
    private static final int MAX_SEATS = 10;

    private final User user;
    private final Show show;
    private final SeatMap map;
    private final JLabel summary = new JLabel(" ");
    private final JLabel notice = new JLabel(" ");
    private final JLabel total = new JLabel(" ");
    private final JButton pay = Theme.primary("Proceed to Payment");
    private final javax.swing.Timer refresher;

    public SeatDialog(Window owner, User user, Show show) {
        super(owner, "Select seats - " + show.movie(), ModalityType.APPLICATION_MODAL);
        this.user = user;
        this.show = show;
        this.map = new SeatMap();

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));

        // header
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel t = Theme.label(show.movie(), Theme.H2.deriveFont(22f), Color.WHITE);
        JLabel sub = Theme.label(show.cinema() + "  |  " + show.screen() + "  |  " + show.date() + "  "
            + show.time().format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)), Theme.BASE, SOFT);
        t.setAlignmentX(LEFT_ALIGNMENT); sub.setAlignmentX(LEFT_ALIGNMENT);
        top.add(t); top.add(Box.createVerticalStrut(2)); top.add(sub);
        root.add(top, BorderLayout.NORTH);

        // seat map
        JScrollPane sp = new JScrollPane(map);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(PANEL);
        sp.getVerticalScrollBar().setUnitIncrement(20);
        JPanel mapCard = new JPanel(new BorderLayout());
        mapCard.setBackground(PANEL);
        mapCard.setBorder(BorderFactory.createLineBorder(new Color(0x1E293B)));
        mapCard.add(sp, BorderLayout.CENTER);
        mapCard.add(legend(), BorderLayout.SOUTH);
        root.add(mapCard, BorderLayout.CENTER);

        // footer
        JPanel foot = new JPanel(new BorderLayout(12, 0));
        foot.setOpaque(false);
        summary.setFont(Theme.BOLD); summary.setForeground(Color.WHITE);
        notice.setFont(Theme.SMALL); notice.setForeground(new Color(0xFBBF24));
        total.setFont(Theme.H2); total.setForeground(Color.WHITE);
        JPanel info = new JPanel(new GridLayout(2, 1));
        info.setOpaque(false);
        info.add(summary); info.add(notice);
        JButton cancel = Theme.secondary("Cancel");
        cancel.addActionListener(e -> dispose());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        btns.add(total); btns.add(cancel); btns.add(pay);
        foot.add(info, BorderLayout.CENTER);
        foot.add(btns, BorderLayout.EAST);
        root.add(foot, BorderLayout.SOUTH);
        pay.addActionListener(e -> checkout());

        setContentPane(root);
        map.reload();
        updateSummary();
        setSize(Math.min(980, Math.max(760, map.getPreferredSize().width + 90)), 700);
        setMinimumSize(new Dimension(700, 520));
        setLocationRelativeTo(owner);

        refresher = new javax.swing.Timer(5000, e -> map.refreshBooked());
        refresher.start();
        addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent e) { refresher.stop(); } });
    }

    private JPanel legend() {
        JPanel l = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 8));
        l.setOpaque(false);
        l.add(chip(SeatMap.REG_FILL, SeatMap.REG_LINE, "Regular " + Theme.money(show.price()).replace(".00", "")));
        l.add(chip(SeatMap.PRE_FILL, SeatMap.PRE_LINE, "Premium " + Theme.money(BookingService.seatPrice(show.price(), "PREMIUM")).replace(".00", "")));
        l.add(chip(Theme.ACCENT, Theme.ACCENT, "Selected"));
        l.add(chip(SeatMap.TAKEN_FILL, SeatMap.TAKEN_FILL, "Booked"));
        return l;
    }

    private JLabel chip(Color fill, Color line, String text) {
        JLabel l = new JLabel(text, new Icon() {
            public void paintIcon(Component c, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(fill); g.fillRoundRect(x, y, 18, 16, 6, 6);
                g.setColor(line); g.drawRoundRect(x, y, 18, 16, 6, 6);
                g.dispose();
            }
            public int getIconWidth() { return 20; }
            public int getIconHeight() { return 16; }
        }, SwingConstants.LEFT);
        l.setFont(Theme.SMALL); l.setForeground(SOFT);
        return l;
    }

    // ---------------------------------------------------------------- state
    private BigDecimal totalPrice() {
        BigDecimal t = BigDecimal.ZERO;
        for (Seat s : map.selectedSeats()) t = t.add(BookingService.seatPrice(show.price(), s.type()));
        return t;
    }

    private void updateSummary() {
        List<Seat> sel = map.selectedSeats();
        if (sel.isEmpty()) {
            summary.setText("Click seats to select them (max " + MAX_SEATS + ")");
            total.setText(" ");
        } else {
            StringJoiner j = new StringJoiner(", ");
            sel.forEach(s -> j.add(s.label()));
            summary.setText(sel.size() + (sel.size() == 1 ? " seat: " : " seats: ") + j);
            total.setText(Theme.money(totalPrice()));
        }
        pay.setEnabled(!sel.isEmpty());
    }

    private void checkout() {
        List<Seat> sel = map.selectedSeats();
        if (sel.isEmpty()) return;
        BigDecimal amount = totalPrice();
        PaymentDialog.Choice c = PaymentDialog.ask(this, amount);
        if (c == null) return;
        try {
            int id = BookingService.book(user.id(), show.id(), sel.stream().map(Seat::id).toList(), c.method(), c.simulateFailure());
            refresher.stop();
            StringJoiner j = new StringJoiner(", ");
            sel.forEach(s -> j.add(s.label()));
            TicketDialog.show(this, id, show, j.toString(), amount, c.method());
            dispose();
        } catch (BookingException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Booking failed", JOptionPane.WARNING_MESSAGE);
            map.reload();
        }
    }

    // ---------------------------------------------------------------- seat map component
    private class SeatMap extends JComponent {
        static final Color REG_FILL = new Color(0x27324A), REG_LINE = new Color(0x4B5B7A),
            PRE_FILL = new Color(0x3A2E0C), PRE_LINE = new Color(0xF59E0B), TAKEN_FILL = new Color(0x1B2236);
        static final int W = 36, H = 32, GAP = 8, ROW_PITCH = 44, LABEL_W = 34, AISLE = 30, TOP = 96;

        private List<Seat> seats = List.of();
        private Set<Integer> taken = new HashSet<>();
        private final Set<Integer> chosen = new LinkedHashSet<>();
        private final Map<String, List<Seat>> rows = new TreeMap<>();
        private Seat hover;

        SeatMap() {
            ToolTipManager.sharedInstance().registerComponent(this);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    Seat s = at(e.getPoint());
                    if (s != hover) {
                        hover = s;
                        setCursor(Cursor.getPredefinedCursor(s != null && !taken.contains(s.id()) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { click(at(e.getPoint())); }
                @Override public void mouseExited(MouseEvent e) { hover = null; repaint(); }
            });
        }

        void reload() {
            try {
                seats = SeatDao.byScreen(show.screenId());
                taken = new HashSet<>(SeatDao.booked(show.id()));
            } catch (SQLException ex) { Theme.error(SeatDialog.this, ex); }
            rows.clear();
            for (Seat s : seats) rows.computeIfAbsent(s.row(), k -> new ArrayList<>()).add(s);
            chosen.removeIf(taken::contains);
            revalidate(); repaint(); updateSummary();
        }

        /** Background refresh: pick up seats booked by other people while this window is open. */
        void refreshBooked() {
            new SwingWorker<Set<Integer>, Void>() {
                @Override protected Set<Integer> doInBackground() throws Exception { return new HashSet<>(SeatDao.booked(show.id())); }
                @Override protected void done() {
                    try {
                        Set<Integer> now = get();
                        if (now.equals(taken)) return;
                        taken = now;
                        if (chosen.removeIf(taken::contains)) notice.setText("Someone just booked one of your selected seats - it was removed.");
                        repaint(); updateSummary();
                    } catch (Exception ignored) {}
                }
            }.execute();
        }

        List<Seat> selectedSeats() { return seats.stream().filter(s -> chosen.contains(s.id())).toList(); }

        private void click(Seat s) {
            if (s == null || taken.contains(s.id())) return;
            notice.setText(" ");
            if (!chosen.remove(s.id())) {
                if (chosen.size() >= MAX_SEATS) { notice.setText("You can select at most " + MAX_SEATS + " seats."); return; }
                chosen.add(s.id());
            }
            repaint(); updateSummary();
        }

        private int maxCols() { return rows.values().stream().mapToInt(List::size).max().orElse(1); }
        private int rowWidth(int n) { return n * W + (n - 1) * GAP + (n >= 6 ? AISLE : 0); }

        @Override public Dimension getPreferredSize() {
            int extra = 0; String prev = null;
            for (var e : rows.entrySet()) {
                String type = e.getValue().get(0).type();
                if (prev != null && !prev.equals(type)) extra += 26;
                prev = type;
            }
            return new Dimension(LABEL_W * 2 + rowWidth(maxCols()) + 60, TOP + rows.size() * ROW_PITCH + extra + 40);
        }

        /** Rectangle for each seat; also used for hit-testing. */
        private Map<Seat, Rectangle2D> seatRects() {
            Map<Seat, Rectangle2D> out = new LinkedHashMap<>();
            int y = TOP; String prev = null;
            for (var e : rows.entrySet()) {
                List<Seat> row = e.getValue();
                String type = row.get(0).type();
                if (prev != null && !prev.equals(type)) y += 26;
                prev = type;
                int n = row.size();
                int x0 = (getWidth() - rowWidth(n)) / 2;
                int x = x0;
                for (int i = 0; i < n; i++) {
                    if (n >= 6 && i == n / 2) x += AISLE;
                    out.put(row.get(i), new Rectangle2D.Double(x, y, W, H));
                    x += W + GAP;
                }
                y += ROW_PITCH;
            }
            return out;
        }

        private Seat at(Point p) {
            for (var e : seatRects().entrySet()) if (e.getValue().contains(p)) return e.getKey();
            return null;
        }

        @Override public String getToolTipText(MouseEvent e) {
            Seat s = at(e.getPoint());
            if (s == null) return null;
            return s.label() + " - " + s.type().toLowerCase() + " - "
                + (taken.contains(s.id()) ? "already booked" : Theme.money(BookingService.seatPrice(show.price(), s.type())));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(PANEL); g.fillRect(0, 0, getWidth(), getHeight());
            int cx = getWidth() / 2;

            // curved screen with glow
            int sw = Math.min(getWidth() - 100, rowWidth(maxCols()) + 60);
            Path2D screen = new Path2D.Double();
            screen.moveTo(cx - sw / 2.0, 52);
            screen.quadTo(cx, 18, cx + sw / 2.0, 52);
            for (int i = 14; i >= 1; i--) {
                g.setColor(new Color(0xE11D48 >> 16 & 255, 0x1D, 0x48, 3 + (14 - i)));
                g.setStroke(new BasicStroke(i * 2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(screen);
            }
            g.setPaint(new GradientPaint(cx, 20, Color.WHITE, cx, 52, new Color(0xFDA4AF)));
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(screen);
            g.setStroke(new BasicStroke(1f));
            g.setFont(Theme.SMALL.deriveFont(Font.BOLD, 11f));
            g.setColor(SOFT);
            drawCentered(g, "S C R E E N   T H I S   W A Y", cx, 78);

            Map<Seat, Rectangle2D> lay = seatRects();
            String prevRow = null, prevType = null;
            for (var e : rows.entrySet()) {
                List<Seat> row = e.getValue();
                Rectangle2D first = lay.get(row.get(0)), last = lay.get(row.get(row.size() - 1));
                String type = row.get(0).type();
                if (!type.equals(prevType) && !(prevType == null && type.equals("REGULAR"))) {
                    g.setFont(Theme.SMALL.deriveFont(Font.BOLD, 11f));
                    g.setColor(type.equals("PREMIUM") ? PRE_LINE : SOFT);
                    drawCentered(g, type + "  -  " + Theme.money(BookingService.seatPrice(show.price(), type)).replace(".00", ""),
                        cx, (int) first.getY() - 10);
                }
                prevType = type; prevRow = e.getKey();
                g.setFont(Theme.BOLD); g.setColor(SOFT);
                g.drawString(e.getKey(), (int) first.getX() - 26, (int) first.getY() + 21);
                g.drawString(e.getKey(), (int) last.getMaxX() + 12, (int) first.getY() + 21);
            }
            for (var e : lay.entrySet()) drawSeat(g, e.getKey(), e.getValue());
            g.dispose();
        }

        private void drawSeat(Graphics2D g, Seat s, Rectangle2D r) {
            boolean isTaken = taken.contains(s.id()), isSel = chosen.contains(s.id()), isHover = s == hover && !isTaken;
            Color fill = isTaken ? TAKEN_FILL : isSel ? Theme.ACCENT : s.isPremium() ? PRE_FILL : REG_FILL;
            Color line = isTaken ? TAKEN_FILL : isSel ? new Color(0xFB7185) : s.isPremium() ? PRE_LINE : REG_LINE;
            if (isHover && !isSel) line = Color.WHITE;
            double x = r.getX(), y = r.getY();
            if (isSel) { g.setColor(new Color(0xE11D48 >> 16 & 255, 0x1D, 0x48, 60)); g.fill(new RoundRectangle2D.Double(x - 3, y - 3, W + 6, H + 6, 14, 14)); }
            // chair: backrest on top + cushion
            Shape back = new RoundRectangle2D.Double(x + 3, y, W - 6, H * 0.62, 10, 10);
            Shape cushion = new RoundRectangle2D.Double(x, y + H * 0.38, W, H * 0.62, 9, 9);
            g.setColor(fill); g.fill(back); g.fill(cushion);
            g.setColor(line); g.setStroke(new BasicStroke(isSel || isHover ? 1.8f : 1.2f));
            g.draw(back); g.draw(cushion);
            g.setFont(Theme.SMALL.deriveFont(Font.BOLD, 11f));
            g.setColor(isTaken ? new Color(0x3B4660) : isSel ? Color.WHITE : s.isPremium() ? new Color(0xFDE68A) : new Color(0xCBD5E1));
            String txt = isTaken ? "x" : String.valueOf(s.number());
            FontMetrics fm = g.getFontMetrics();
            g.drawString(txt, (float) (x + (W - fm.stringWidth(txt)) / 2.0), (float) (y + H * 0.38 + (H * 0.62 + fm.getAscent() - fm.getDescent()) / 2.0));
            g.setStroke(new BasicStroke(1f));
        }

        private void drawCentered(Graphics2D g, String s, int cx, int y) {
            g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, y);
        }
    }
}
