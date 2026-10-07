package ui;

import model.User;
import service.AuthService;
import util.Theme;
import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final JTextField email = new JTextField(20);
    private final JPasswordField pass = new JPasswordField(20);

    public LoginFrame() {
        super("CineBook - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 540);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2));

        // left: branding
        JPanel left = new JPanel(new GridBagLayout());
        left.setBackground(Theme.DARK);
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("<html><span style='color:white'>CINE</span><span style='color:#E11D48'>BOOK</span></html>");
        logo.setFont(Theme.H1.deriveFont(44f));
        JLabel tag = Theme.label("<html>Pick a movie. Pick your seats.<br>Enjoy the show.</html>", Theme.BASE.deriveFont(16f), new Color(0xCBD5E1));
        box.add(logo); box.add(Box.createVerticalStrut(14)); box.add(tag);
        left.add(box);

        // right: form
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Color.WHITE);
        JPanel f = new JPanel();
        f.setOpaque(false);
        f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));
        f.add(Theme.label("Welcome back", Theme.H1, Theme.TEXT));
        f.add(Box.createVerticalStrut(4));
        f.add(Theme.label("Log in to book your tickets", Theme.BASE, Theme.MUTED));
        f.add(Box.createVerticalStrut(22));
        f.add(left("Email")); f.add(email); f.add(Box.createVerticalStrut(12));
        f.add(left("Password")); f.add(pass); f.add(Box.createVerticalStrut(20));
        JButton login = Theme.primary("Log in");
        login.setAlignmentX(LEFT_ALIGNMENT);
        login.addActionListener(e -> doLogin());
        f.add(login);
        f.add(Box.createVerticalStrut(10));
        JButton reg = Theme.secondary("Create new account");
        reg.setAlignmentX(LEFT_ALIGNMENT);
        reg.addActionListener(e -> new RegisterDialog(this).setVisible(true));
        f.add(reg);
        f.add(Box.createVerticalStrut(22));
        f.add(Theme.label("<html>Demo: admin@cinema.com / admin123<br>Customer: rahul@mail.com / user123</html>", Theme.SMALL, Theme.MUTED));
        for (Component c : f.getComponents()) if (c instanceof JComponent jc) jc.setAlignmentX(LEFT_ALIGNMENT);
        right.add(f);

        add(left); add(right);
        getRootPane().setDefaultButton(login);
    }

    private static JLabel left(String t) {
        JLabel l = Theme.label(t, Theme.BOLD, Theme.TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private void doLogin() {
        try {
            User u = AuthService.login(email.getText(), new String(pass.getPassword()));
            dispose();
            (u.isAdmin() ? new AdminFrame(u) : new CustomerFrame(u)).setVisible(true);
        } catch (Exception ex) { Theme.error(this, ex); }
    }
}
