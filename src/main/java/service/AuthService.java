package service;

import dao.UserDao;
import model.User;
import util.Passwords;
import java.sql.SQLIntegrityConstraintViolationException;

public final class AuthService {
    private AuthService() {}

    public static User login(String email, String password) throws Exception {
        if (email.isBlank() || password.isEmpty()) throw new IllegalArgumentException("Enter email and password.");
        return UserDao.login(email.trim().toLowerCase(), Passwords.sha256(password))
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
    }

    public static void register(String name, String email, String phone, String password) throws Exception {
        if (name.isBlank()) throw new IllegalArgumentException("Name is required.");
        if (!email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) throw new IllegalArgumentException("Enter a valid email.");
        if (!phone.isBlank() && !phone.matches("\\d{10}")) throw new IllegalArgumentException("Phone must be 10 digits.");
        if (password.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters.");
        try {
            UserDao.register(name.trim(), email.trim().toLowerCase(), phone.trim(), Passwords.sha256(password));
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("This email is already registered.");
        }
    }
}
