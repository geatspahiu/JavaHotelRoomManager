package com.hotel.service;

import com.hotel.dao.RoomDAO;
import com.hotel.model.HotelStats;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import com.hotel.model.RoomTypeStats;

import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RoomService {
    private static final int MAX_ROOM_BATCH_SIZE = 250;
    private final RoomDAO roomDAO = new RoomDAO();

    public void save(Room room) throws SQLException {
        validate(room);
        if (room.getId() == 0) {
            roomDAO.create(room);
        } else {
            roomDAO.update(room);
        }
    }

    public void createRoomRange(String startRoomNumber, int count, RoomType type,
                                BigDecimal price, boolean available) throws SQLException {
        if (startRoomNumber == null || startRoomNumber.isBlank()) {
            throw new IllegalArgumentException("Starting room number is required.");
        }
        if (!startRoomNumber.matches("\\d+")) {
            throw new IllegalArgumentException("Starting room number must be numeric.");
        }
        if (count < 1) {
            throw new IllegalArgumentException("Room count must be at least 1.");
        }
        if (count > MAX_ROOM_BATCH_SIZE) {
            throw new IllegalArgumentException("Room count cannot exceed " + MAX_ROOM_BATCH_SIZE + ".");
        }

        int start;
        try {
            start = Integer.parseInt(startRoomNumber);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Starting room number is too large.");
        }
        if ((long) start + count - 1 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Room range is too large.");
        }
        int width = startRoomNumber.length();
        List<Room> rooms = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String roomNumber = String.format("%0" + width + "d", start + i);
            Room room = new Room(0, roomNumber, type, price, available);
            validate(room);
            rooms.add(room);
        }
        roomDAO.createMany(rooms);
    }

    public void delete(int id) throws SQLException {
        roomDAO.delete(id);
    }

    public List<Room> findAll() throws SQLException {
        return roomDAO.findAll();
    }

    public List<Room> search(RoomType type, Boolean available) throws SQLException {
        return roomDAO.search(type, available);
    }

    public List<Room> findAvailableRooms() throws SQLException {
        return roomDAO.findAvailableRooms();
    }

    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut) throws SQLException {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }
        return roomDAO.findAvailableRooms(checkIn, checkOut);
    }

    public HotelStats getStats() throws SQLException {
        return roomDAO.getStats();
    }

    public List<RoomTypeStats> getStatsByType() throws SQLException {
        return roomDAO.getStatsByType();
    }

    private void validate(Room room) {
        if (room.getRoomNumber() == null || room.getRoomNumber().isBlank()) {
            throw new IllegalArgumentException("Room number is required.");
        }
        if (room.getType() == null) {
            throw new IllegalArgumentException("Room type is required.");
        }
        if (room.getPrice() == null || room.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Price must be zero or greater.");
        }
        if (room.getPrice().scale() > 2) {
            throw new IllegalArgumentException("Price can have at most two decimal places.");
        }
    }
}
