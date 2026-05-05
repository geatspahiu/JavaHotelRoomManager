package com.hotel.service;

import com.hotel.dao.RoomDAO;
import com.hotel.model.HotelStats;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import com.hotel.model.RoomTypeStats;

import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RoomService {
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

        int start = Integer.parseInt(startRoomNumber);
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
    }
}
