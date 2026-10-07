# Cinema Ticket Booking System (Java Swing + MySQL)

A DBMS mini-project: customers browse movies, pick a cinema/show, choose seats on a seat map, pay (simulated) and manage bookings;
admins manage the whole catalogue and view revenue reports. Booking runs in a **single ACID transaction** with **double-booking prevention**.

## Tech stack
Java 17+ · Swing (+ FlatLaf look & feel) · MySQL 8 · JDBC (MySQL Connector/J) · Maven

## Setup (5 steps)
1. Install JDK 17+, Maven and MySQL 8.
2. Create the database:
   ```
   mysql -u root -p < database/schema.sql
   mysql -u root -p < database/seed.sql
   ```
3. Set your DB credentials (defaults: user `root`, empty password, `localhost:3306`):
   - Windows (cmd): `set CINEMA_DB_USER=root` and `set CINEMA_DB_PASSWORD=yourpassword`
   - Linux/Mac: `export CINEMA_DB_PASSWORD=yourpassword`
   - Optional: `CINEMA_DB_URL` (full JDBC URL).
   - In IntelliJ/Eclipse: add them as environment variables in the Run Configuration of `Main`.
4. Run: `mvn compile exec:java`  (or open the folder as a Maven project in IntelliJ/Eclipse and run `Main`)
5. Build a runnable jar: `mvn package` then `java -jar target/cinema-booking-1.0.jar`

## Demo logins
| Role | Email | Password |
|---|---|---|
| Admin | admin@cinema.com | admin123 |
| Customer | rahul@mail.com | user123 |

## Project structure
```
src/main/java/
 ├── Main.java
 ├── model/    records: User, Movie, Cinema, Screen, Seat, Show, Booking
 ├── dao/      JDBC data access (Movie, Cinema, Screen, Seat, Show, User, Booking, Report)
 ├── service/  AuthService, BookingService (transactions), BookingException
 ├── ui/       Swing screens (Login, Customer, Admin, SeatDialog, PaymentDialog ...)
 └── util/     Db (JDBC helper), Theme (look & feel), Passwords (SHA-256), Table
database/  schema.sql · seed.sql · queries.sql
docs/      ER_Diagram.md
REPORT.md
```

## Demo script for the viva
1. **Customer flow:** login as Rahul → search "sci" → pick a show → select seats → pay → see confirmation → *My Bookings*.
2. **Double booking:** run the app twice (two windows), open the same show in both, select the same seat in both, pay in A, then pay in B → B gets "already booked".
3. **Rollback:** tick *Simulate payment failure* in the payment dialog → booking and seats are NOT saved (check *My Bookings* / `SELECT * FROM bookings`).
4. **Admin:** login as admin → Dashboard shows KPIs and 6 reports **with the SQL used** (GROUP BY, HAVING, subquery, joins). Try deleting a movie that has shows → blocked by the foreign key.
5. **SQL:** open `database/queries.sql` and run section by section.

## Where each DBMS concept lives
| Concept | Location |
|---|---|
| PK / FK / UNIQUE / NOT NULL / CHECK, composite key | `database/schema.sql` (`booking_seats` PK = booking_id + seat_id) |
| Views | `v_show_details`, `v_revenue_by_movie` |
| Indexes | `idx_show_movie_date`, `idx_booking_user`, `idx_booking_show_status`, `idx_bs_seat` |
| Trigger (extra safety) | `trg_prevent_double_booking` |
| JOIN / GROUP BY / HAVING / subqueries | `ReportDao.java`, `queries.sql` |
| Transactions, COMMIT/ROLLBACK, row locking | `BookingService.java` |
| CRUD | `dao/*` + Admin tabs |
