package ui;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/** Small reusable form popup (text fields and drop-downs). Returns null if cancelled. */
public final class FormDialog {
    private FormDialog() {}

    public record Field(String label, Object initial, Object[] options) {
        public Field(String label, Object initial) { this(label, initial, null); }
    }

    public static Map<String, Object> show(Component parent, String title, Field... fields) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.anchor = GridBagConstraints.WEST;
        Map<String, JComponent> comps = new LinkedHashMap<>();
        int row = 0;
        for (Field f : fields) {
            g.gridx = 0; g.gridy = row; g.fill = GridBagConstraints.NONE;
            p.add(new JLabel(f.label()), g);
            JComponent c;
            if (f.options() != null) {
                JComboBox<Object> box = new JComboBox<>(f.options());
                box.setSelectedItem(f.initial());
                c = box;
            } else {
                c = new JTextField(f.initial() == null ? "" : f.initial().toString(), 24);
            }
            g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL;
            p.add(c, g);
            comps.put(f.label(), c);
            row++;
        }
        int r = JOptionPane.showConfirmDialog(parent, p, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return null;
        Map<String, Object> out = new LinkedHashMap<>();
        for (var e : comps.entrySet()) {
            JComponent c = e.getValue();
            out.put(e.getKey(), c instanceof JComboBox<?> b ? b.getSelectedItem() : ((JTextField) c).getText().trim());
        }
        return out;
    }

    public static String s(Map<String, Object> m, String key) { return String.valueOf(m.get(key)).trim(); }
}
