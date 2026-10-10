import ui.LoginFrame;
import util.Db;
import util.Theme;
import javax.swing.*;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        Theme.install();
        try (Connection c = Db.get()) {
            // connection OK
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Cannot connect to MySQL:\n" + e.getMessage() +
                "\n\nCheck that MySQL is running, database/schema.sql + seed.sql were executed,\n" +
                "and set CINEMA_DB_USER / CINEMA_DB_PASSWORD (or CINEMA_DB_URL) environment variables.",
                "Database error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        try { service.ShowScheduler.ensure(10); }
        catch (Exception e) { System.err.println("Could not auto-generate shows: " + e.getMessage()); }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
