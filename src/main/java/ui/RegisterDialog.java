package ui;

import service.AuthService;
import util.Theme;
import javax.swing.*;
import java.awt.*;

public class RegisterDialog extends JDialog {
    public RegisterDialog(Window owner) {
        super(owner, "Create account", ModalityType.APPLICATION_MODAL);
        JTextField name = new JTextField(22), email = new JTextField(22), phone = new JTextField(22);
        JPasswordField pass = new JPasswordField(22), pass2 = new JPasswordField(22);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4); g.anchor = GridBagConstraints.WEST; g.gridwidth = 2;
        g.gridx = 0; g.gridy = 0;
        p.add(Theme.label("Create your account", Theme.H2, Theme.TEXT), g);
        String[] labels = {"Full name", "Email", "Phone (10 digits, optional)", "Password (min 6)", "Confirm password"};
        JComponent[] fields = {name, email, phone, pass, pass2};
        g.gridwidth = 1; g.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < labels.length; i++) {
            g.gridy = i + 1; g.gridx = 0; g.weightx = 0; p.add(new JLabel(labels[i]), g);
            g.gridx = 1; g.weightx = 1; p.add(fields[i], g);
        }
        JButton reg = Theme.primary("Register");
        g.gridx = 0; g.gridy = 7; g.gridwidth = 2;
        p.add(reg, g);
        reg.addActionListener(e -> {
            try {
                String a = new String(pass.getPassword()), b = new String(pass2.getPassword());
                if (!a.equals(b)) throw new IllegalArgumentException("Passwords do not match.");
                AuthService.register(name.getText(), email.getText(), phone.getText(), a);
                JOptionPane.showMessageDialog(this, "Account created! You can now log in.");
                dispose();
            } catch (Exception ex) { Theme.error(this, ex); }
        });
        getRootPane().setDefaultButton(reg);
        setContentPane(p);
        pack();
        setLocationRelativeTo(owner);
    }
}
