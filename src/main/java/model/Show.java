package model;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
public record Show(int id, int movieId, int screenId, int cinemaId, LocalDate date, LocalTime time,
                   BigDecimal price, String movie, String cinema, String screen) {}
