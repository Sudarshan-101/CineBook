package model;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
public record Booking(int id, String movie, String cinema, String screen, LocalDate date, LocalTime time,
                      String seats, BigDecimal total, String status) {}
