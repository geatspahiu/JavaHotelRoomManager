package com.hotel.service;

import com.hotel.config.DatabaseConnection;
import com.hotel.dao.BookingDAO;
import com.hotel.dao.GuestDAO;
import com.hotel.dao.RoomDAO;
import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;
import com.hotel.model.Guest;
import com.hotel.model.Room;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class BookingService {
    private final BookingDAO bookingDAO = new BookingDAO();
    private final GuestDAO guestDAO = new GuestDAO();
    private final RoomDAO roomDAO = new RoomDAO();

    public void bookRoom(Booking booking) throws SQLException {
        validate(booking);
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Room room = roomDAO.findById(connection, booking.getRoomId())
                        .orElseThrow(() -> new IllegalArgumentException("Selected room does not exist."));
                if (!room.isAvailable()) {
                    throw new IllegalArgumentException("Selected room is not available.");
                }
                booking.setStatus(BookingStatus.ACTIVE);
                bookingDAO.create(connection, booking);
                roomDAO.setAvailability(connection, booking.getRoomId(), false);
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void bookRoomWithGuest(Booking booking, Guest guest) throws SQLException {
        bookRoomWithGuests(booking, guest, List.of());
    }

    public void bookRoomWithGuests(Booking booking, Guest mainGuest, List<Guest> additionalGuests) throws SQLException {
        validateGuest(mainGuest);
        for (Guest guest : additionalGuests) {
            validateGuest(guest);
        }
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (mainGuest.getId() == 0) {
                    int guestId = guestDAO.create(connection, mainGuest);
                    booking.setGuestId(guestId);
                } else {
                    booking.setGuestId(mainGuest.getId());
                }

                validate(booking);
                Room room = roomDAO.findById(connection, booking.getRoomId())
                        .orElseThrow(() -> new IllegalArgumentException("Selected room does not exist."));
                if (!room.isAvailable()) {
                    throw new IllegalArgumentException("Selected room is not available.");
                }
                booking.setStatus(BookingStatus.ACTIVE);
                int bookingId = bookingDAO.createAndReturnId(connection, booking);
                bookingDAO.addBookingGuest(connection, bookingId, booking.getGuestId(), true);
                for (Guest guest : additionalGuests) {
                    int guestId = guest.getId() == 0 ? guestDAO.create(connection, guest) : guest.getId();
                    if (guestId != booking.getGuestId()) {
                        bookingDAO.addBookingGuest(connection, bookingId, guestId, false);
                    }
                }
                roomDAO.setAvailability(connection, booking.getRoomId(), false);
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void completeBooking(int bookingId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Booking booking = bookingDAO.findById(connection, bookingId);
                if (booking.getStatus() == BookingStatus.COMPLETED) {
                    throw new IllegalArgumentException("Booking is already completed.");
                }
                bookingDAO.markCompleted(connection, bookingId);
                roomDAO.setAvailability(connection, booking.getRoomId(), true);
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<Booking> findAll() throws SQLException {
        syncExpiredBookings();
        return bookingDAO.findAll();
    }

    public List<Booking> findByStatus(BookingStatus status) throws SQLException {
        syncExpiredBookings();
        if (status == null) {
            return bookingDAO.findAll();
        }
        return bookingDAO.findByStatus(status);
    }

    public void syncExpiredBookings() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                LocalDate today = LocalDate.now();
                List<Integer> expiredRoomIds = bookingDAO.findExpiredActiveRoomIds(connection, today);
                bookingDAO.markExpiredCompleted(connection, today);
                roomDAO.setAvailabilityForRooms(connection, expiredRoomIds, true);
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void validate(Booking booking) {
        if (booking.getRoomId() == 0) {
            throw new IllegalArgumentException("Room is required.");
        }
        if (booking.getGuestId() == 0) {
            throw new IllegalArgumentException("Guest is required.");
        }
        if (booking.getCheckIn() == null || booking.getCheckOut() == null) {
            throw new IllegalArgumentException("Check-in and check-out dates are required.");
        }
        if (!booking.getCheckOut().isAfter(booking.getCheckIn())) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }
    }

    private void validateGuest(Guest guest) {
        if (guest == null) {
            throw new IllegalArgumentException("Guest is required.");
        }
        if (guest.getName() == null || guest.getName().isBlank()) {
            throw new IllegalArgumentException("Guest name is required.");
        }
        if (guest.getPhone() == null || guest.getPhone().isBlank()) {
            throw new IllegalArgumentException("Phone number is required.");
        }
        if (guest.getEmail() == null || guest.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
    }
}
