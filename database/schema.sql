-- =====================================================================
--  Cinema Ticket Booking System  |  MySQL 8  |  3NF schema
--  Run:  mysql -u root -p < database/schema.sql
-- =====================================================================
DROP DATABASE IF EXISTS cinema_db;
CREATE DATABASE cinema_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cinema_db;

CREATE TABLE users (
  user_id       INT AUTO_INCREMENT PRIMARY KEY,
  full_name     VARCHAR(100) NOT NULL,
  email         VARCHAR(120) NOT NULL UNIQUE,
  phone         VARCHAR(15),
  password_hash CHAR(64)     NOT NULL,                       -- SHA-256 hex
  role          ENUM('CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE movies (
  movie_id     INT AUTO_INCREMENT PRIMARY KEY,
  title        VARCHAR(150) NOT NULL UNIQUE,
  genre        VARCHAR(50),
  language     VARCHAR(30),
  duration_min INT NOT NULL CHECK (duration_min > 0),
  rating       VARCHAR(10),
  release_date DATE,
  description  VARCHAR(500)
) ENGINE=InnoDB;

CREATE TABLE cinemas (
  cinema_id INT AUTO_INCREMENT PRIMARY KEY,
  name      VARCHAR(100) NOT NULL,
  city      VARCHAR(60)  NOT NULL,
  address   VARCHAR(200),
  UNIQUE (name, city)
) ENGINE=InnoDB;

CREATE TABLE screens (
  screen_id   INT AUTO_INCREMENT PRIMARY KEY,
  cinema_id   INT NOT NULL,
  screen_name VARCHAR(40) NOT NULL,
  UNIQUE (cinema_id, screen_name),
  FOREIGN KEY (cinema_id) REFERENCES cinemas(cinema_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE seats (
  seat_id     INT AUTO_INCREMENT PRIMARY KEY,
  screen_id   INT NOT NULL,
  seat_row    CHAR(1) NOT NULL,
  seat_number INT NOT NULL CHECK (seat_number > 0),
  seat_type   ENUM('REGULAR','PREMIUM') NOT NULL DEFAULT 'REGULAR',
  UNIQUE (screen_id, seat_row, seat_number),
  FOREIGN KEY (screen_id) REFERENCES screens(screen_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE shows (
  show_id   INT AUTO_INCREMENT PRIMARY KEY,
  movie_id  INT NOT NULL,
  screen_id INT NOT NULL,
  show_date DATE NOT NULL,
  show_time TIME NOT NULL,
  price     DECIMAL(8,2) NOT NULL CHECK (price > 0),       -- base (REGULAR) price
  UNIQUE (screen_id, show_date, show_time),                -- a screen can't run two shows at once
  FOREIGN KEY (movie_id)  REFERENCES movies(movie_id)   ON DELETE RESTRICT,
  FOREIGN KEY (screen_id) REFERENCES screens(screen_id) ON DELETE RESTRICT,
  INDEX idx_show_movie_date (movie_id, show_date)
) ENGINE=InnoDB;

CREATE TABLE bookings (
  booking_id   INT AUTO_INCREMENT PRIMARY KEY,
  user_id      INT NOT NULL,
  show_id      INT NOT NULL,
  booking_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 0),
  status       ENUM('CONFIRMED','CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
  FOREIGN KEY (user_id) REFERENCES users(user_id),
  FOREIGN KEY (show_id) REFERENCES shows(show_id),
  INDEX idx_booking_user (user_id),
  INDEX idx_booking_show_status (show_id, status)
) ENGINE=InnoDB;

CREATE TABLE booking_seats (                               -- composite primary key
  booking_id INT NOT NULL,
  seat_id    INT NOT NULL,
  price      DECIMAL(8,2) NOT NULL,
  PRIMARY KEY (booking_id, seat_id),
  FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
  FOREIGN KEY (seat_id)    REFERENCES seats(seat_id),
  INDEX idx_bs_seat (seat_id)
) ENGINE=InnoDB;

CREATE TABLE payments (
  payment_id INT AUTO_INCREMENT PRIMARY KEY,
  booking_id INT NOT NULL UNIQUE,                          -- 1 booking : 1 payment
  amount     DECIMAL(10,2) NOT NULL,
  method     ENUM('CARD','UPI','NET_BANKING') NOT NULL,
  status     ENUM('SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'SUCCESS',
  paid_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------ VIEWS ---------------------------------
CREATE VIEW v_show_details AS
SELECT sh.show_id, sh.movie_id, m.title, c.cinema_id, c.name AS cinema_name, c.city,
       sh.screen_id, sc.screen_name, sh.show_date, sh.show_time, sh.price
FROM shows sh
JOIN movies  m  ON m.movie_id   = sh.movie_id
JOIN screens sc ON sc.screen_id = sh.screen_id
JOIN cinemas c  ON c.cinema_id  = sc.cinema_id;

CREATE VIEW v_revenue_by_movie AS
SELECT m.movie_id, m.title, COUNT(DISTINCT b.booking_id) AS bookings,
       COUNT(bs.seat_id) AS tickets, SUM(bs.price) AS revenue
FROM bookings b
JOIN booking_seats bs ON bs.booking_id = b.booking_id
JOIN shows  sh ON sh.show_id  = b.show_id
JOIN movies m  ON m.movie_id  = sh.movie_id
WHERE b.status = 'CONFIRMED'
GROUP BY m.movie_id, m.title;

-- ----------- Double-booking safety net (2nd line of defence) -----------
DELIMITER $$
CREATE TRIGGER trg_prevent_double_booking
BEFORE INSERT ON booking_seats FOR EACH ROW
BEGIN
  DECLARE v_show INT;
  DECLARE v_cnt  INT;
  SELECT show_id INTO v_show FROM bookings WHERE booking_id = NEW.booking_id;
  SELECT COUNT(*) INTO v_cnt
    FROM booking_seats bs JOIN bookings b ON b.booking_id = bs.booking_id
   WHERE b.show_id = v_show AND b.status = 'CONFIRMED' AND bs.seat_id = NEW.seat_id;
  IF v_cnt > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Seat already booked for this show';
  END IF;
END$$
DELIMITER ;
