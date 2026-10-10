USE cinema_db;

-- Passwords: admin123 (admin) / user123 (customers) -- stored as SHA-256
INSERT INTO users (user_id, full_name, email, phone, password_hash, role) VALUES
 (1,'System Admin','admin@cinema.com','9000000001',SHA2('admin123',256),'ADMIN'),
 (2,'Rahul Sharma','rahul@mail.com','9000000002',SHA2('user123',256),'CUSTOMER'),
 (3,'Priya Patil','priya@mail.com','9000000003',SHA2('user123',256),'CUSTOMER'),
 (4,'Amit Verma','amit@mail.com','9000000004',SHA2('user123',256),'CUSTOMER'),
 (5,'Sneha Kulkarni','sneha@mail.com','9000000005',SHA2('user123',256),'CUSTOMER');

INSERT INTO movies (movie_id,title,genre,language,duration_min,rating,release_date,description) VALUES
 (1,'Inception','Sci-Fi','English',148,'UA','2010-07-16','A thief who steals secrets through dreams is offered one last impossible job.'),
 (2,'Interstellar','Sci-Fi','English',169,'UA','2014-11-07','Explorers travel through a wormhole in search of a new home for humanity.'),
 (3,'3 Idiots','Comedy','Hindi',170,'U','2009-12-25','Three engineering students and a friendship that challenges the rat race.'),
 (4,'The Dark Knight','Action','English',152,'UA','2008-07-18','Batman faces the Joker, a criminal mastermind who wants to plunge Gotham into chaos.'),
 (5,'Dangal','Drama','Hindi',161,'U','2016-12-23','A former wrestler trains his daughters to become world-class champions.'),
 (6,'Spirited Away','Animation','Japanese',125,'U','2001-07-20','A girl wanders into a world of spirits and must find a way to free her parents.'),
 (7,'Oppenheimer','Drama','English',180,'UA','2023-07-21','The story of J. Robert Oppenheimer and the creation of the atomic bomb.'),
 (8,'RRR','Action','Telugu',187,'UA','2022-03-25','Two legendary revolutionaries join forces against British rule in 1920s India.'),
 (9,'Coco','Animation','English',105,'U','2017-11-22','A boy who dreams of being a musician journeys into the vibrant Land of the Dead.'),
 (10,'Gully Boy','Drama','Hindi',153,'UA','2019-02-14','A street rapper from the slums of Mumbai rises to chase his dream.'),
 (11,'Kantara','Thriller','Kannada',148,'UA','2022-09-30','A tale of folklore, forest land and a conflict between man and nature.'),
 (12,'Top Gun: Maverick','Action','English',130,'UA','2022-05-27','After thirty years, Maverick trains a new generation of elite fighter pilots.');

INSERT INTO cinemas (cinema_id,name,city,address) VALUES
 (1,'PVR Phoenix','Pune','Viman Nagar, Pune'),
 (2,'INOX Seasons','Mumbai','Andheri West, Mumbai'),
 (3,'Cinepolis City Centre','Nashik','City Centre Mall, College Road, Nashik'),
 (4,'Carnival Cinemas','Nashik','Trimbak Road, Nashik');

INSERT INTO screens (screen_id,cinema_id,screen_name) VALUES
 (1,1,'Screen 1'),(2,1,'Screen 2'),(3,2,'Screen 1'),(4,2,'IMAX'),
 (5,3,'Screen 1'),(6,3,'Gold Class'),(7,4,'Screen 1'),(8,4,'Screen 2');

-- 8 rows (A-H) x 10 seats per screen; rows F,G,H are PREMIUM
INSERT INTO seats (screen_id, seat_row, seat_number, seat_type)
SELECT s.screen_id, r.r, n.n, IF(r.r IN ('F','G','H'),'PREMIUM','REGULAR')
FROM screens s
JOIN (SELECT 'A' r UNION ALL SELECT 'B' UNION ALL SELECT 'C' UNION ALL SELECT 'D'
      UNION ALL SELECT 'E' UNION ALL SELECT 'F' UNION ALL SELECT 'G' UNION ALL SELECT 'H') r
JOIN (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
      UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10) n;

-- A few hand-made shows for the next 3 days (sample bookings below use them).
-- The app ALSO auto-generates shows for the next ~10 days on every start (see ShowScheduler.java),
-- so the show list never goes stale.
INSERT INTO shows (show_id,movie_id,screen_id,show_date,show_time,price) VALUES
 (1,1,1,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'10:00:00',250),
 (2,1,1,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'18:30:00',300),
 (3,2,2,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'14:00:00',250),
 (4,3,3,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'11:00:00',220),
 (5,4,4,DATE_ADD(CURDATE(),INTERVAL 1 DAY),'20:00:00',320),
 (6,5,1,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'14:00:00',250),
 (7,6,2,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'18:00:00',250),
 (8,2,3,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'16:00:00',230),
 (9,1,4,DATE_ADD(CURDATE(),INTERVAL 2 DAY),'12:00:00',300),
 (10,3,1,DATE_ADD(CURDATE(),INTERVAL 3 DAY),'19:00:00',260),
 (11,4,2,DATE_ADD(CURDATE(),INTERVAL 3 DAY),'15:00:00',240),
 (12,5,3,DATE_ADD(CURDATE(),INTERVAL 3 DAY),'21:00:00',280);

-- Sample bookings so the reports have data (premium seats cost 1.5x)
INSERT INTO bookings (booking_id,user_id,show_id,booking_time,total_amount,status) VALUES
 (1,2,1,DATE_SUB(NOW(),INTERVAL 5 DAY), 500.00,'CONFIRMED'),
 (2,3,1,DATE_SUB(NOW(),INTERVAL 4 DAY), 500.00,'CONFIRMED'),
 (3,2,3,DATE_SUB(NOW(),INTERVAL 3 DAY), 750.00,'CONFIRMED'),
 (4,4,2,DATE_SUB(NOW(),INTERVAL 2 DAY), 900.00,'CONFIRMED'),
 (5,5,4,DATE_SUB(NOW(),INTERVAL 1 DAY), 220.00,'CONFIRMED'),
 (6,3,5,DATE_SUB(NOW(),INTERVAL 1 DAY), 960.00,'CANCELLED');

INSERT INTO booking_seats (booking_id,seat_id,price)
SELECT 1,seat_id,250 FROM seats WHERE screen_id=1 AND seat_row='C' AND seat_number IN (4,5);
INSERT INTO booking_seats (booking_id,seat_id,price)
SELECT 2,seat_id,250 FROM seats WHERE screen_id=1 AND seat_row='B' AND seat_number IN (1,2);
INSERT INTO booking_seats (booking_id,seat_id,price)
SELECT 3,seat_id,250 FROM seats WHERE screen_id=2 AND seat_row='A' AND seat_number IN (1,2,3);
INSERT INTO booking_seats (booking_id,seat_id,price)
SELECT 4,seat_id,450 FROM seats WHERE screen_id=1 AND seat_row='F' AND seat_number IN (3,4);
INSERT INTO booking_seats (booking_id,seat_id,price)
SELECT 5,seat_id,220 FROM seats WHERE screen_id=3 AND seat_row='D' AND seat_number=5;
INSERT INTO booking_seats (booking_id,seat_id,price)
SELECT 6,seat_id,480 FROM seats WHERE screen_id=4 AND seat_row='F' AND seat_number IN (2,3);

INSERT INTO payments (booking_id,amount,method,status) VALUES
 (1,500,'CARD','SUCCESS'),(2,500,'UPI','SUCCESS'),(3,750,'CARD','SUCCESS'),
 (4,900,'NET_BANKING','SUCCESS'),(5,220,'UPI','SUCCESS'),(6,960,'CARD','REFUNDED');
