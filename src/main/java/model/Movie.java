package model;
import java.time.LocalDate;
public record Movie(int id, String title, String genre, String language, int duration, String rating,
                    LocalDate releaseDate, String description) {
    @Override public String toString() { return title; }
}
