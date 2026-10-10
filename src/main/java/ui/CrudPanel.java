package ui;

import util.Table;
import util.Theme;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Generic admin table with Add / Edit / Delete / Refresh. First column must be the numeric ID. */
public abstract class CrudPanel extends JPanel {
    protected final DefaultTableModel model = Theme.model();
    protected final JTable table = new JTable(model);
    protected final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    protected final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

    protected CrudPanel(boolean canAdd, boolean canEdit, boolean canDelete) {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(Theme.BG);
        Theme.table(table);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        top.setOpaque(false);
        buttons.setOpaque(false);
        JPanel card = Theme.card(new BorderLayout(0, 10));
        card.add(top, BorderLayout.NORTH);
        card.add(Theme.scroll(table), BorderLayout.CENTER);
        card.add(buttons, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);

        JButton refresh = Theme.secondary("Refresh");
        refresh.addActionListener(e -> refresh());
        buttons.add(refresh);
        if (canDelete) {
            JButton b = Theme.secondary("Delete selected");
            b.addActionListener(e -> run(true, row -> delete(row)));
            buttons.add(b);
        }
        if (canEdit) {
            JButton b = Theme.secondary("Edit selected");
            b.addActionListener(e -> run(true, row -> edit(row)));
            buttons.add(b);
        }
        if (canAdd) {
            JButton b = Theme.primary("Add new");
            b.addActionListener(e -> run(false, row -> add()));
            buttons.add(b);
        }
    }

    protected interface Action { void go(Object[] row) throws Exception; }

    protected abstract Table load() throws Exception;
    protected void add() throws Exception {}
    protected void edit(Object[] row) throws Exception {}
    protected void delete(Object[] row) throws Exception {}

    public void refresh() {
        try { Theme.fill(model, load()); } catch (Exception e) { Theme.error(this, e); }
    }

    private void run(boolean needsRow, Action a) {
        try {
            Object[] row = null;
            if (needsRow) {
                row = selected();
                if (row == null) return;
            }
            a.go(row);
            refresh();
        } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Invalid input format: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            Theme.error(this, e);
        }
    }

    protected Object[] selected() {
        int r = table.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Select a row first."); return null; }
        r = table.convertRowIndexToModel(r);
        Object[] row = new Object[model.getColumnCount()];
        for (int i = 0; i < row.length; i++) row[i] = model.getValueAt(r, i);
        return row;
    }

    protected static int id(Object[] row) { return ((Number) row[0]).intValue(); }

    protected boolean confirmDelete() {
        return JOptionPane.showConfirmDialog(this, "Delete the selected record?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
