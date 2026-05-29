package com.hotel.dao;

import com.hotel.config.DatabaseConnection;
import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {
    public void create(Connection connection, Booking booking) throws SQLException {
        String sql = "INSERT INTO bookings (room_id, guest_id, check_in, check_out, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, booking.getRoomId());
            statement.setInt(2, booking.getGuestId());
            statement.setDate(3, Date.valueOf(booking.getCheckIn()));
            statement.setDate(4, Date.valueOf(booking.getCheckOut()));
            statement.setString(5, booking.getStatus().name());
            statement.executeUpdate();
        }
    }

    public int createAndReturnId(Connection connection, Booking booking) throws SQLException {
        String sql = "INSERT INTO bookings (room_id, guest_id, check_in, check_out, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, booking.getRoomId());
            statement.setInt(2, booking.getGuestId());
            statement.setDate(3, Date.valueOf(booking.getCheckIn()));
            statement.setDate(4, Date.valueOf(booking.getCheckOut()));
            statement.setString(5, booking.getStatus().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Creating booking failed, no ID returned.");
            }
        }
    }

    public void addBookingGuest(Connection connection, int bookingId, int guestId, boolean mainGuest) throws SQLException {
        ensureBookingGuestsTable(connection);
        String sql = "INSERT INTO booking_guests (booking_id, guest_id, main_guest) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookingId);
            statement.setInt(2, guestId);
            statement.setBoolean(3, mainGuest);
            statement.executeUpdate();
        }
    }

    private void ensureBookingGuestsTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS booking_guests (
                    booking_id INT NOT NULL,
                    guest_id INT NOT NULL,
                    main_guest BOOLEAN NOT NULL DEFAULT FALSE,
                    PRIMARY KEY (booking_id, guest_id),
                    CONSTRAINT fk_booking_guests_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
                        ON UPDATE CASCADE ON DELETE CASCADE,
                    CONSTRAINT fk_booking_guests_guest FOREIGN KEY (guest_id) REFERENCES guests(id)
                        ON UPDATE CASCADE ON DELETE RESTRICT
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public List<Booking> findAll() throws SQLException {
        String sql = baseSelect() + " ORDER BY b.check_in DESC";
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return mapBookings(resultSet);
        }
    }

    public List<Booking> findByStatus(BookingStatus status) throws SQLException {
        String sql = baseSelect() + " WHERE b.status = ? ORDER BY b.check_in DESC";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapBookings(resultSet);
            }
        }
    }

    public void markCompleted(Connection connection, int bookingId) throws SQLException {
        String sql = "UPDATE bookings SET status = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, BookingStatus.COMPLETED.name());
            statement.setInt(2, bookingId);
            statement.executeUpdate();
        }
    }

    public boolean hasActiveOverlapForRoom(Connection connection, int roomId, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = """
                SELECT 1
                FROM bookings
                WHERE room_id = ?
                  AND status = ?
                  AND check_in < ?
                  AND check_out > ?
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomId);
            statement.setString(2, BookingStatus.ACTIVE.name());
            statement.setDate(3, Date.valueOf(checkOut));
            statement.setDate(4, Date.valueOf(checkIn));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean hasActiveBookingForRoomOnDate(Connection connection, int roomId, LocalDate date) throws SQLException {
        String sql = """
                SELECT 1
                FROM bookings
                WHERE room_id = ?
                  AND status = ?
                  AND check_in <= ?
                  AND check_out > ?
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomId);
            statement.setString(2, BookingStatus.ACTIVE.name());
            statement.setDate(3, Date.valueOf(date));
            statement.setDate(4, Date.valueOf(date));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public List<Integer> findExpiredActiveRoomIds(Connection connection, java.time.LocalDate today) throws SQLException {
        String sql = "SELECT room_id FROM bookings WHERE status = ? AND check_out < ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, BookingStatus.ACTIVE.name());
            statement.setDate(2, Date.valueOf(today));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Integer> roomIds = new ArrayList<>();
                while (resultSet.next()) {
                    roomIds.add(resultSet.getInt("room_id"));
                }
                return roomIds;
            }
        }
    }

    public void markExpiredCompleted(Connection connection, java.time.LocalDate today) throws SQLException {
        String sql = "UPDATE bookings SET status = ? WHERE status = ? AND check_out < ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, BookingStatus.COMPLETED.name());
            statement.setString(2, BookingStatus.ACTIVE.name());
            statement.setDate(3, Date.valueOf(today));
            statement.executeUpdate();
        }
    }

    public Booking findById(Connection connection, int bookingId) throws SQLException {
        String sql = baseSelect() + " WHERE b.id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookingId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapBooking(resultSet);
                }
                throw new SQLException("Booking not found with id " + bookingId);
            }
        }
    }

    public Booking findByIdForUpdate(Connection connection, int bookingId) throws SQLException {
        String sql = baseSelect() + " WHERE b.id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookingId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapBooking(resultSet);
                }
                throw new SQLException("Booking not found with id " + bookingId);
            }
        }
    }

    private String baseSelect() {
        return """
                SELECT b.*, r.room_number, g.name AS guest_name
                FROM bookings b
                JOIN rooms r ON r.id = b.room_id
                JOIN guests g ON g.id = b.guest_id
                """;
    }

    private List<Booking> mapBookings(ResultSet resultSet) throws SQLException {
        List<Booking> bookings = new ArrayList<>();
        while (resultSet.next()) {
            bookings.add(mapBooking(resultSet));
        }
        return bookings;
    }

    private Booking mapBooking(ResultSet resultSet) throws SQLException {
        return new Booking(
                resultSet.getInt("id"),
                resultSet.getInt("room_id"),
                resultSet.getInt("guest_id"),
                resultSet.getString("room_number"),
                resultSet.getString("guest_name"),
                resultSet.getDate("check_in").toLocalDate(),
                resultSet.getDate("check_out").toLocalDate(),
                BookingStatus.valueOf(resultSet.getString("status"))
        );
    }
}
