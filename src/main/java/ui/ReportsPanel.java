package ui;

import dao.ReportDao;
import util.Table;
import util.Theme;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Admin dashboard: KPI cards + revenue / analytics reports (with the SQL shown). */
public class ReportsPanel extends JPanel {
    private final JLabel[] kpi = new JLabel[4];
    private final JComboBox<String> box = new JComboBox<>(ReportDao.REPORTS.keySet().toArray(new String[0]));
    private final DefaultTableModel model = Theme.model();
    private final JTextArea sql = new JTextArea(6, 40);

    public ReportsPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(Theme.BG);

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);
        String[] names = {"Customers", "Confirmed bookings", "Net revenue", "Movies"};
        for (int i = 0; i < 4; i++) {
            JPanel c = Theme.card(new GridLayout(2, 1));
            c.add(Theme.label(names[i], Theme.BASE, Theme.MUTED));
            kpi[i] = Theme.label("-", Theme.H1, i == 2 ? Theme.ACCENT : Theme.TEXT);
            c.add(kpi[i]);
            cards.add(c);
        }
        add(cards, BorderLayout.NORTH);

        JTable table = new JTable(model);
        Theme.table(table);
        table.setAutoCreateRowSorter(true);
        JPanel card = Theme.card(new BorderLayout(0, 10));
        JPanel pick = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pick.setOpaque(false);
        pick.add(Theme.label("Report:", Theme.BOLD, Theme.TEXT));
        pick.add(box);
        card.add(pick, BorderLayout.NORTH);
        card.add(Theme.scroll(table), BorderLayout.CENTER);
        sql.setEditable(false);
        sql.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        sql.setBackground(new Color(0xF8FAFC));
        JScrollPane sp = Theme.scroll(sql);
        sp.setPreferredSize(new Dimension(100, 130));
        card.add(sp, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
        box.addActionListener(e -> run());
    }

    public void refresh() {
        try {
            Object[] r = ReportDao.summary().rows().get(0);
            for (int i = 0; i < 4; i++) kpi[i].setText(i == 2 ? Theme.money(new java.math.BigDecimal(r[i].toString())) : r[i].toString());
        } catch (Exception e) { Theme.error(this, e); }
        run();
    }

    private void run() {
        String key = (String) box.getSelectedItem();
        if (key == null) return;
        try {
            Table t = ReportDao.run(key);
            Theme.fill(model, t);
            sql.setText(ReportDao.REPORTS.get(key));
            sql.setCaretPosition(0);
        } catch (Exception e) { Theme.error(this, e); }
    }
}
