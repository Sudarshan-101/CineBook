package model;
public record Seat(int id, int screenId, String row, int number, String type) {
    public String label() { return row + number; }
    public boolean isPremium() { return "PREMIUM".equals(type); }
}
