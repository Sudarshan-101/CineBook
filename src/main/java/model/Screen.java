package model;
public record Screen(int id, int cinemaId, String name, String cinemaName) {
    @Override public String toString() { return cinemaName + " - " + name; }
}
