package ui;

import util.Theme;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

/** Simulated payment gateway. */
public final class PaymentDialog {
    private PaymentDialog() {}
    public record Choice(String method, boolean simulateFailure) {}

    public static Choice ask(Component parent, BigDecimal amount) {
        JComboBox<String> method = new JComboBox<>(new String[]{"CARD", "UPI", "NET_BANKING"});
        JTextField detail = new JTextField(20);
        JLabel detailLabel = new JLabel("Card number (any 16 digits)");
        JCheckBox fail = new JCheckBox("Simulate payment failure (demonstrates ROLLBACK)");
        method.addActionListener(e -> detailLabel.setText(switch ((String) method.getSelectedItem()) {
            case "UPI" -> "UPI ID (e.g. name@bank)";
            case "NET_BANKING" -> "Bank name";
            default -> "Card number (any 16 digits)";
        }));
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4); g.anchor = GridBagConstraints.WEST; g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        p.add(Theme.label("Amount payable: " + Theme.money(amount), Theme.H2, Theme.ACCENT), g);
        g.gridwidth = 1; g.gridy = 1; p.add(new JLabel("Payment method"), g); g.gridx = 1; p.add(method, g);
        g.gridx = 0; g.gridy = 2; p.add(detailLabel, g); g.gridx = 1; p.add(detail, g);
        g.gridx = 0; g.gridy = 3; g.gridwidth = 2; p.add(fail, g);
        g.gridy = 4; p.add(Theme.label("This is a simulation - no real payment is made.", Theme.SMALL, Theme.MUTED), g);
        while (true) {
            int r = JOptionPane.showConfirmDialog(parent, p, "Payment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) return null;
            if (detail.getText().isBlank()) {
                JOptionPane.showMessageDialog(parent, "Please fill in the payment detail.");
                continue;
            }
            return new Choice((String) method.getSelectedItem(), fail.isSelected());
        }
    }
}
