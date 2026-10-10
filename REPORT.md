# Cinema Ticket Booking System Using Java and MySQL
**DBMS Mini-Project Report**

Name: ____________  Roll No: ____________  Class: ____________  Guide: ____________

## 1. Introduction
Cinema ticket booking involves many connected entities - movies, cinemas, screens, seats, shows, customers, bookings and payments - and many users acting at the same time. This project builds a desktop application (Java Swing) backed by a normalized MySQL database that lets customers book seats online and lets administrators manage the cinema and track revenue.

## 2. Problem Statement
Manual or spreadsheet-based ticketing leads to duplicate seat sales, lost records, no payment trail and no reporting. A reliable system must guarantee that **a seat is never sold twice** and that a booking, its seats and its payment are saved **all together or not at all**.

## 3. Objectives
- Design a 3NF relational database for cinema ticketing.
- Provide customer features: register/login, search movies, choose cinema/show/seats, pay, view/cancel bookings.
- Provide admin features: manage movies, cinemas, screens, seats and shows; view users, bookings and revenue reports.
- Demonstrate transactions (COMMIT/ROLLBACK), ACID properties and double-booking prevention.
- Demonstrate SQL: joins, GROUP BY/HAVING, aggregates, subqueries, views, indexes.

## 4. Existing System
Counter-based or spreadsheet booking: seat availability is checked by hand, two clerks can sell the same seat, cancellations are not reflected instantly, and revenue reports must be compiled manually.

## 5. Proposed System
A Java Swing client connects through JDBC to MySQL. All business rules that must never break (unique seats per show, foreign keys, valid prices) are enforced **in the database**; the booking operation runs as **one transaction** with row locking. The UI shows a live seat map (booked seats greyed out) and an admin dashboard with SQL-driven reports.

## 6. Technology Stack
| Layer | Technology |
|---|---|
| Language | Java 17 |
| GUI | Java Swing (FlatLaf look & feel) |
| Database | MySQL 8 (InnoDB) |
| Connectivity | JDBC, MySQL Connector/J 8.4 |
| Build | Maven |
| IDE | IntelliJ IDEA / Eclipse |
| Architecture | Layered: model - DAO - service - UI |

## 7. ER Diagram
See `docs/ER_Diagram.md` for the Mermaid diagram. Text form:

```
USERS 1---N BOOKINGS N---1 SHOWS N---1 MOVIES
                |             |
                |             N---1 SCREENS N---1 CINEMAS
                |                     |
 BOOKINGS 1---1 PAYMENTS              1
 BOOKINGS 1---N BOOKING_SEATS N---1 SEATS N---1 SCREENS
```
Relationships: a cinema has many screens; a screen has many seats and hosts many shows; a movie has many shows; a user makes many bookings; a booking is for one show, includes many seats (M:N between bookings and seats resolved by `booking_seats`) and has one payment.

## 8. Database Schema
```
USERS(user_id PK, full_name, email UNIQUE, phone, password_hash, role, created_at)
MOVIES(movie_id PK, title UNIQUE, genre, language, duration_min, rating, release_date, description)
CINEMAS(cinema_id PK, name, city, address)                      UNIQUE(name, city)
SCREENS(screen_id PK, cinema_id FK, screen_name)                UNIQUE(cinema_id, screen_name)
SEATS(seat_id PK, screen_id FK, seat_row, seat_number, seat_type)  UNIQUE(screen_id, seat_row, seat_number)
SHOWS(show_id PK, movie_id FK, screen_id FK, show_date, show_time, price)  UNIQUE(screen_id, show_date, show_time)
BOOKINGS(booking_id PK, user_id FK, show_id FK, booking_time, total_amount, status)
BOOKING_SEATS(booking_id FK, seat_id FK, price)                 PK(booking_id, seat_id)
PAYMENTS(payment_id PK, booking_id FK UNIQUE, amount, method, status, paid_at)
```
Constraints: PK/FK on every table, NOT NULL on mandatory columns, UNIQUE on email/title/seat position/screen timetable, CHECK on prices and durations, ENUMs for roles/status/method. Foreign keys use `ON DELETE RESTRICT` where history must be protected (movies, shows, users, bookings->seats) and `CASCADE` where children cannot exist alone (cinema->screens->seats, booking->booking_seats/payment).
**Views:** `v_show_details`, `v_revenue_by_movie`. **Indexes:** `idx_show_movie_date`, `idx_booking_user`, `idx_booking_show_status`, `idx_bs_seat`. **Trigger:** `trg_prevent_double_booking`.

## 9. Normalization
**1NF** - every column holds a single atomic value; no repeating groups. Seats of a booking are *not* stored as a list like "A1,A2" but as separate rows in `booking_seats`.
**2NF** - no partial dependency. The only composite key is `booking_seats(booking_id, seat_id)`; its non-key attribute `price` (price paid for that seat in that booking) depends on the whole key.
**3NF** - no transitive dependency. Movie details live in `movies`, not in `shows`; cinema city lives in `cinemas`, not in `screens`; customer details live in `users`, not in `bookings`. Example: if `bookings` stored `cinema_name`, then `booking_id -> show_id -> screen_id -> cinema_id -> cinema_name` would be transitive, so it is kept out. The attribute `total_amount` is stored deliberately as a historical snapshot of the amount charged (prices may change later) and is verified against the sum of `booking_seats.price` at booking time.

## 10. SQL Queries
Full set in `database/queries.sql`. Highlights:
```sql
-- JOIN (4 tables)
SELECT m.title, c.name, sc.screen_name, sh.show_date, sh.show_time, sh.price
FROM shows sh JOIN movies m ON m.movie_id=sh.movie_id
JOIN screens sc ON sc.screen_id=sh.screen_id JOIN cinemas c ON c.cinema_id=sc.cinema_id;

-- GROUP BY + HAVING + aggregates
SELECT m.title, SUM(bs.price) revenue, COUNT(*) tickets FROM booking_seats bs
JOIN bookings b ON b.booking_id=bs.booking_id AND b.status='CONFIRMED'
JOIN shows sh ON sh.show_id=b.show_id JOIN movies m ON m.movie_id=sh.movie_id
GROUP BY m.movie_id, m.title HAVING SUM(bs.price) > 500;

-- Subquery: customers who spent more than the average customer
... HAVING SUM(b.total_amount) > (SELECT AVG(t.total) FROM (SELECT SUM(total_amount) total
     FROM bookings WHERE status='CONFIRMED' GROUP BY user_id) t);

-- Seats already taken for a show
SELECT bs.seat_id FROM booking_seats bs JOIN bookings b ON b.booking_id=bs.booking_id
WHERE b.show_id=? AND b.status='CONFIRMED';
```

## 11. Transactions & ACID
Booking steps (`BookingService.book`) in one transaction:
```
START TRANSACTION
 1. SELECT ... FROM shows WHERE show_id=? FOR UPDATE      -- lock the show
 2. validate seats belong to the show's screen, compute price
 3. check none of the seats is already CONFIRMED for this show
 4. INSERT bookings   5. INSERT booking_seats   6. INSERT payments
COMMIT   (any error or failed payment -> ROLLBACK)
```
**Double-booking prevention** has three layers: (1) the `FOR UPDATE` lock on the show row makes two simultaneous bookings of the same show execute one after another; (2) the availability check runs while holding that lock, so the second user sees the first user's seats as taken; (3) the `BEFORE INSERT` trigger on `booking_seats` rejects a duplicate even if application code were bypassed. Cancelled bookings keep their rows but are ignored by the availability check, so their seats become free again.

| Property | How it is achieved |
|---|---|
| Atomicity | booking + seats + payment commit together or roll back together (e.g. simulated payment failure leaves no trace) |
| Consistency | PK/FK/UNIQUE/CHECK constraints and the trigger keep data valid |
| Isolation | InnoDB row locks + READ COMMITTED; concurrent bookings of one show are serialized |
| Durability | committed data is written to the InnoDB redo log and survives restart |

## 12. System Screenshots
*(Insert screenshots after running the application)*
1. Login screen  2. Register dialog  3. Browse movies & shows  4. Seat selection map  5. Payment dialog  6. Booking confirmation  7. My Bookings  8. Admin dashboard & reports  9. Admin CRUD tabs (Movies, Shows, Seats)

## 13. Testing
| # | Test | Expected | Result |
|---|---|---|---|
| 1 | Login with valid / invalid credentials | Opens home / error message | Pass |
| 2 | Register with duplicate email | "Email already registered" | Pass |
| 3 | Book 2 seats and pay | Booking, 2 booking_seats rows and 1 payment row created | Pass |
| 4 | Book a seat that was just taken | Rejected with message, seat map refreshed | Pass |
| 5 | 8 users book the same seat simultaneously | Exactly 1 succeeds | Pass |
| 6 | Payment failure (simulated) | Full rollback, no rows saved | Pass |
| 7 | Cancel booking | Status CANCELLED, payment REFUNDED, seats free again | Pass |
| 8 | Delete movie that has shows | Blocked by foreign key | Pass |
| 9 | Admin reports | All 6 reports return data | Pass |
Tests 3-9 were executed against MySQL 8 with the service layer; GUI tests were done by manual use.

## 14. Advantages
Correct under concurrent use; fully normalized; business rules enforced by the database; clean layered code; simple desktop UI with a visual seat map; built-in revenue analytics.

## 15. Limitations
Desktop-only (no web/mobile); payment is simulated; passwords use unsalted SHA-256 (use bcrypt/Argon2 in production); one currency; no e-mail/SMS tickets; no overlap check for show durations on the same screen (only identical start times are blocked); a new database connection is opened per operation (no connection pool).

## 16. Future Scope
Web or mobile front end (Spring Boot REST + React); real payment gateway; seat-hold timer (e.g. 5 minutes) before payment; QR-code e-tickets; discount coupons and loyalty points; dynamic pricing; connection pooling (HikariCP); salted password hashing.

## 17. Conclusion
The project delivers a working, database-driven cinema booking system that demonstrates core DBMS concepts - ER modelling, 3NF design, constraints, joins, aggregation, views, indexes and, most importantly, ACID transactions that prevent double booking. The layered Java design keeps SQL in DAOs and business rules in services, making the system easy to maintain and extend.
