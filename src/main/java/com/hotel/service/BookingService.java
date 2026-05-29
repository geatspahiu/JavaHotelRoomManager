package com.hotel.service;

import com.hotel.config.DatabaseConnection;
import com.hotel.dao.BookingDAO;
import com.hotel.dao.GuestDAO;
import com.hotel.dao.RoomDAO;
import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;
import com.hotel.model.Guest;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BookingService {
    private static final String EMAIL_PATTERN = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$";
    private static final String PHONE_PATTERN = "^[+()0-9 .-]{7,30}$";
    private final BookingDAO bookingDAO = new BookingDAO();
    private final GuestDAO guestDAO = new GuestDAO();
    private final RoomDAO roomDAO = new RoomDAO();

    public void bookRoom(Booking booking) throws SQLException {
        validate(booking);
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                roomDAO.findByIdForUpdate(connection, booking.getRoomId())
                        .orElseThrow(() -> new IllegalArgumentException("Selected room does not exist."));
                if (bookingDAO.hasActiveOverlapForRoom(connection, booking.getRoomId(), booking.getCheckIn(), booking.getCheckOut())) {
                    throw new IllegalArgumentException("Selected room is already booked for these dates.");
                }
                booking.setStatus(BookingStatus.ACTIVE);
                bookingDAO.create(connection, booking);
                updateLegacyAvailabilityForToday(connection, booking.getRoomId());
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
        if (additionalGuests == null) {
            additionalGuests = List.of();
        }
        Guest bookingMainGuest = copyGuest(mainGuest);
        validateGuest(bookingMainGuest);
        for (Guest guest : additionalGuests) {
            validateGuest(guest);
        }
        validateDatesAndRoom(booking);
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                roomDAO.findByIdForUpdate(connection, booking.getRoomId())
                        .orElseThrow(() -> new IllegalArgumentException("Selected room does not exist."));
                if (bookingDAO.hasActiveOverlapForRoom(connection, booking.getRoomId(), booking.getCheckIn(), booking.getCheckOut())) {
                    throw new IllegalArgumentException("Selected room is already booked for these dates.");
                }

                if (bookingMainGuest.getId() == 0) {
                    int guestId = guestDAO.create(connection, bookingMainGuest);
                    booking.setGuestId(guestId);
                } else {
                    guestDAO.findById(connection, bookingMainGuest.getId());
                    booking.setGuestId(bookingMainGuest.getId());
                }

                validate(booking);
                booking.setStatus(BookingStatus.ACTIVE);
                int bookingId = bookingDAO.createAndReturnId(connection, booking);
                bookingDAO.addBookingGuest(connection, bookingId, booking.getGuestId(), true);
                Set<Integer> linkedGuests = new HashSet<>();
                linkedGuests.add(booking.getGuestId());
                for (Guest guest : additionalGuests) {
                    Guest additionalGuest = copyGuest(guest);
                    int guestId = additionalGuest.getId() == 0 ? guestDAO.create(connection, additionalGuest) : additionalGuest.getId();
                    if (additionalGuest.getId() != 0) {
                        guestDAO.findById(connection, additionalGuest.getId());
                    }
                    if (linkedGuests.add(guestId)) {
                        bookingDAO.addBookingGuest(connection, bookingId, guestId, false);
                    }
                }
                updateLegacyAvailabilityForToday(connection, booking.getRoomId());
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
                Booking booking = bookingDAO.findByIdForUpdate(connection, bookingId);
                roomDAO.findByIdForUpdate(connection, booking.getRoomId())
                        .orElseThrow(() -> new IllegalArgumentException("Selected room does not exist."));
                if (booking.getStatus() == BookingStatus.COMPLETED) {
                    throw new IllegalArgumentException("Booking is already completed.");
                }
                bookingDAO.markCompleted(connection, bookingId);
                updateLegacyAvailabilityForToday(connection, booking.getRoomId());
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
                for (Integer roomId : expiredRoomIds) {
                    roomDAO.findByIdForUpdate(connection, roomId);
                    updateLegacyAvailabilityForToday(connection, roomId);
                }
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

    private void validateDatesAndRoom(Booking booking) {
        if (booking.getRoomId() == 0) {
            throw new IllegalArgumentException("Room is required.");
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
        if (!guest.getEmail().trim().toUpperCase().matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Email format is invalid.");
        }
        if (!guest.getPhone().trim().matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException("Phone number format is invalid.");
        }
    }

    private void updateLegacyAvailabilityForToday(Connection connection, int roomId) throws SQLException {
        LocalDate today = LocalDate.now();
        boolean occupiedToday = bookingDAO.hasActiveBookingForRoomOnDate(connection, roomId, today);
        roomDAO.setAvailability(connection, roomId, !occupiedToday);
    }

    private Guest copyGuest(Guest guest) {
        if (guest == null) {
            return null;
        }
        return new Guest(
                guest.getId(),
                guest.getName() == null ? null : guest.getName().trim(),
                guest.getPhone() == null ? null : guest.getPhone().trim(),
                guest.getEmail() == null ? null : guest.getEmail().trim()
        );
    }
}
