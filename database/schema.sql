CREATE DATABASE IF NOT EXISTS hotel_management;
USE hotel_management;

CREATE TABLE IF NOT EXISTS rooms (
    id INT PRIMARY KEY AUTO_INCREMENT,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    type ENUM('Single', 'Double', 'Suite') NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS guests (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(120) NOT NULL
);

CREATE TABLE IF NOT EXISTS bookings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    room_id INT NOT NULL,
    guest_id INT NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    status ENUM('ACTIVE', 'COMPLETED') NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_bookings_room FOREIGN KEY (room_id) REFERENCES rooms(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_guest FOREIGN KEY (guest_id) REFERENCES guests(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE INDEX idx_rooms_type ON rooms(type);
CREATE INDEX idx_rooms_available ON rooms(available);
CREATE INDEX idx_bookings_status ON bookings(status);

CREATE TABLE IF NOT EXISTS booking_guests (
    booking_id INT NOT NULL,
    guest_id INT NOT NULL,
    main_guest BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (booking_id, guest_id),
    CONSTRAINT fk_booking_guests_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_booking_guests_guest FOREIGN KEY (guest_id) REFERENCES guests(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);
