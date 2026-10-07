package model;
public record User(int id, String name, String email, String phone, String role) {
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
