USE cinema_db;

-- ============ 1. CRUD ============
INSERT INTO movies (title,genre,language,duration_min,rating) VALUES ('Demo Movie','Drama','English',120,'U');
SELECT * FROM movies WHERE title = 'Demo Movie';
UPDATE movies SET rating = 'UA' WHERE title = 'Demo Movie';
DELETE FROM movies WHERE title = 'Demo Movie';

-- ============ 2. JOINS ============
-- INNER JOIN (4 tables): upcoming shows with movie, cinema and screen
SELECT m.title, c.name AS cinema, sc.screen_name, sh.show_date, sh.show_time, sh.price
FROM shows sh
JOIN movies  m  ON m.movie_id   = sh.movie_id
JOIN screens sc ON sc.screen_id = sh.screen_id
JOIN cinemas c  ON c.cinema_id  = sc.cinema_id
ORDER BY sh.show_date, sh.show_time;

-- LEFT JOIN: every user with number of bookings (0 for users who never booked)
SELECT u.full_name, COUNT(b.booking_id) AS bookings
FROM users u LEFT JOIN bookings b ON b.user_id = u.user_id
GROUP BY u.user_id, u.full_name;

-- Booking details with the seats booked (JOIN + GROUP_CONCAT)
SELECT b.booking_id, u.full_name, v.title, v.cinema_name, v.show_date, v.show_time,
       GROUP_CONCAT(CONCAT(se.seat_row, se.seat_number) ORDER BY se.seat_row, se.seat_number) AS seats,
       b.total_amount, p.method, p.status AS payment_status
FROM bookings b
JOIN users u          ON u.user_id    = b.user_id
JOIN v_show_details v ON v.show_id    = b.show_id
JOIN booking_seats bs ON bs.booking_id = b.booking_id
JOIN seats se         ON se.seat_id   = bs.seat_id
JOIN payments p       ON p.booking_id = b.booking_id
GROUP BY b.booking_id, u.full_name, v.title, v.cinema_name, v.show_date, v.show_time, b.total_amount, p.method, p.status;

-- ============ 3. GROUP BY / HAVING / AGGREGATES ============
SELECT m.title, SUM(bs.price) AS revenue, COUNT(bs.seat_id) AS tickets, AVG(bs.price) AS avg_ticket,
       MIN(bs.price) AS min_price, MAX(bs.price) AS max_price
FROM booking_seats bs
JOIN bookings b ON b.booking_id = bs.booking_id AND b.status = 'CONFIRMED'
JOIN shows sh ON sh.show_id = b.show_id
JOIN movies m ON m.movie_id = sh.movie_id
GROUP BY m.movie_id, m.title
HAVING SUM(bs.price) > 500
ORDER BY revenue DESC;

SELECT DATE(booking_time) AS day, COUNT(*) AS bookings, SUM(total_amount) AS revenue
FROM bookings WHERE status = 'CONFIRMED' GROUP BY DATE(booking_time) ORDER BY day;

-- ============ 4. SUBQUERIES ============
-- Scalar subquery: shows priced above the average
SELECT show_id, title, price FROM v_show_details WHERE price > (SELECT AVG(price) FROM shows);

-- IN subquery: users who booked a Sci-Fi movie
SELECT full_name FROM users WHERE user_id IN (
  SELECT b.user_id FROM bookings b JOIN shows sh ON sh.show_id = b.show_id
  JOIN movies m ON m.movie_id = sh.movie_id WHERE m.genre = 'Sci-Fi');

-- NOT EXISTS: movies that have never been booked
SELECT m.title FROM movies m WHERE NOT EXISTS (
  SELECT 1 FROM shows sh JOIN bookings b ON b.show_id = sh.show_id WHERE sh.movie_id = m.movie_id);

-- Correlated subquery: seats still free for each show
SELECT v.show_id, v.title, v.show_date,
  (SELECT COUNT(*) FROM seats s WHERE s.screen_id = v.screen_id)
  - (SELECT COUNT(*) FROM bookings b JOIN booking_seats bs ON bs.booking_id = b.booking_id
      WHERE b.show_id = v.show_id AND b.status = 'CONFIRMED') AS seats_left
FROM v_show_details v;

-- ============ 5. VIEWS ============
SELECT * FROM v_show_details;
SELECT * FROM v_revenue_by_movie ORDER BY revenue DESC;

-- ============ 6. INDEXES ============
SHOW INDEX FROM bookings;
EXPLAIN SELECT * FROM bookings WHERE user_id = 2;               -- uses idx_booking_user
EXPLAIN SELECT * FROM shows WHERE movie_id = 1 AND show_date >= CURDATE();  -- uses idx_show_movie_date

-- ============ 7. TRANSACTIONS / ACID / DOUBLE-BOOKING ============
-- Run in TWO MySQL sessions to see the lock in action.
-- Session A:
START TRANSACTION;
SELECT * FROM shows WHERE show_id = 6 FOR UPDATE;      -- locks the show row
-- Session B (same statement) now WAITS until Session A commits/rolls back.
INSERT INTO bookings (user_id, show_id, total_amount) VALUES (2, 6, 250);
INSERT INTO booking_seats (booking_id, seat_id, price)
  VALUES (LAST_INSERT_ID(), (SELECT seat_id FROM seats WHERE screen_id=1 AND seat_row='A' AND seat_number=1), 250);
INSERT INTO payments (booking_id, amount, method) VALUES ((SELECT MAX(booking_id) FROM bookings), 250, 'UPI');
COMMIT;      -- or ROLLBACK; to undo everything

-- Double booking attempt: same seat, same show -> trigger raises 'Seat already booked for this show'
START TRANSACTION;
INSERT INTO bookings (user_id, show_id, total_amount) VALUES (3, 6, 250);
INSERT INTO booking_seats (booking_id, seat_id, price)
  VALUES (LAST_INSERT_ID(), (SELECT seat_id FROM seats WHERE screen_id=1 AND seat_row='A' AND seat_number=1), 250);
ROLLBACK;

-- Referential integrity: deleting a movie that has shows is blocked (ON DELETE RESTRICT)
DELETE FROM movies WHERE movie_id = 1;   -- ERROR 1451
