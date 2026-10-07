package util;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Look & feel helpers: colours, fonts, styled components. */
public final class Theme {
    private Theme() {}
    public static final Color DARK = new Color(0x0F172A), ACCENT = new Color(0xE11D48), BG = new Color(0xF1F5F9),
        TEXT = new Color(0x0F172A), MUTED = new Color(0x64748B), BORDER = new Color(0xE2E8F0), OK = new Color(0x16A34A);
    public static final Font BASE = new Font("Segoe UI", Font.PLAIN, 14), BOLD = BASE.deriveFont(Font.BOLD),
        H1 = BASE.deriveFont(Font.BOLD, 28f), H2 = BASE.deriveFont(Font.BOLD, 18f), SMALL = BASE.deriveFont(12f);

    public static void install() {
        UIManager.put("defaultFont", BASE);
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("TabbedPane.selectedBackground", Color.WHITE);
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
        } catch (Exception e) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        }
    }

    public static JButton primary(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT); b.setForeground(Color.WHITE); b.setFont(BOLD);
        b.setFocusPainted(false); b.setMargin(new Insets(8, 18, 8, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton secondary(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.WHITE); b.setForeground(TEXT); b.setFont(BASE);
        b.setFocusPainted(false); b.setMargin(new Insets(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JLabel label(String text, Font f, Color c) {
        JLabel l = new JLabel(text); l.setFont(f); l.setForeground(c); return l;
    }

    public static JPanel card(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        return p;
    }

    public static JPanel header(String right, Runnable logout) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(DARK);
        p.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        JLabel logo = new JLabel("<html><span style='color:white'>CINE</span><span style='color:#E11D48'>BOOK</span></html>");
        logo.setFont(H2);
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        r.setOpaque(false);
        r.add(label(right, BASE, new Color(0xCBD5E1)));
        JButton out = secondary("Logout");
        out.addActionListener(e -> logout.run());
        r.add(out);
        p.add(logo, BorderLayout.WEST);
        p.add(r, BorderLayout.EAST);
        return p;
    }

    public static void table(JTable t) {
        t.setRowHeight(30);
        t.setShowVerticalLines(false);
        t.setGridColor(BORDER);
        t.setSelectionBackground(new Color(0xFFE4E6));
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.getTableHeader().setFont(BOLD);
        t.getTableHeader().setReorderingAllowed(false);
    }

    public static DefaultTableModel model() {
        return new DefaultTableModel() {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    public static void fill(DefaultTableModel m, Table t) {
        m.setColumnIdentifiers(t.cols());
        m.setRowCount(0);
        for (Object[] r : t.rows()) m.addRow(r);
    }

    public static JScrollPane scroll(Component c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(BorderFactory.createLineBorder(BORDER));
        s.getViewport().setBackground(Color.WHITE);
        return s;
    }

    public static String money(BigDecimal v) { return "Rs. " + v.setScale(2, RoundingMode.HALF_UP); }

    public static void error(Component parent, Exception e) {
        String m = e.getMessage();
        if (e instanceof java.sql.SQLIntegrityConstraintViolationException)
            m = "Blocked by a database constraint (duplicate value or record is still referenced):\n" + m;
        JOptionPane.showMessageDialog(parent, m, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
