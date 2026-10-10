package model;
public record Cinema(int id, String name, String city, String address) {
    @Override public String toString() { return name + " (" + city + ")"; }
}
