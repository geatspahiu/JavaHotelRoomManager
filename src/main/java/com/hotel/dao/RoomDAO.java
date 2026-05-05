package com.hotel.dao;

import com.hotel.config.DatabaseConnection;
import com.hotel.model.HotelStats;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import com.hotel.model.RoomTypeStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomDAO {
    public void create(Room room) throws SQLException {
        String sql = "INSERT INTO rooms (room_number, type, price, available) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillRoomStatement(statement, room);
            statement.executeUpdate();
        }
    }

    public void createMany(List<Room> rooms) throws SQLException {
        String sql = "INSERT INTO rooms (room_number, type, price, available) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Room room : rooms) {
                    fillRoomStatement(statement, room);
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void update(Room room) throws SQLException {
        String sql = "UPDATE rooms SET room_number = ?, type = ?, price = ?, available = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillRoomStatement(statement, room);
            statement.setInt(5, room.getId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM rooms WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public List<Room> findAll() throws SQLException {
        String sql = "SELECT * FROM rooms ORDER BY room_number";
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return mapRooms(resultSet);
        }
    }

    public List<Room> search(RoomType type, Boolean available) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM rooms WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (type != null) {
            sql.append(" AND type = ?");
            params.add(type.getDatabaseValue());
        }
        if (available != null) {
            sql.append(" AND available = ?");
            params.add(available);
        }
        sql.append(" ORDER BY room_number");

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapRooms(resultSet);
            }
        }
    }

    public Optional<Room> findById(int id) throws SQLException {
        String sql = "SELECT * FROM rooms WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRoom(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public Optional<Room> findById(Connection connection, int id) throws SQLException {
        String sql = "SELECT * FROM rooms WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRoom(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public List<Room> findAvailableRooms() throws SQLException {
        String sql = "SELECT * FROM rooms WHERE available = TRUE ORDER BY room_number";
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return mapRooms(resultSet);
        }
    }

    public void setAvailability(Connection connection, int roomId, boolean available) throws SQLException {
        String sql = "UPDATE rooms SET available = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, available);
            statement.setInt(2, roomId);
            statement.executeUpdate();
        }
    }

    public void setAvailabilityForRooms(Connection connection, List<Integer> roomIds, boolean available) throws SQLException {
        if (roomIds.isEmpty()) {
            return;
        }

        String sql = "UPDATE rooms SET available = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Integer roomId : roomIds) {
                statement.setBoolean(1, available);
                statement.setInt(2, roomId);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public HotelStats getStats() throws SQLException {
        String sql = """
                SELECT
                    COUNT(*) AS total_rooms,
                    SUM(CASE WHEN available = FALSE THEN 1 ELSE 0 END) AS occupied_rooms,
                    SUM(CASE WHEN available = TRUE THEN 1 ELSE 0 END) AS available_rooms
                FROM rooms
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return new HotelStats(
                        resultSet.getInt("total_rooms"),
                        resultSet.getInt("occupied_rooms"),
                        resultSet.getInt("available_rooms")
                );
            }
            return new HotelStats(0, 0, 0);
        }
    }

    public List<RoomTypeStats> getStatsByType() throws SQLException {
        String sql = """
                SELECT type,
                    SUM(CASE WHEN available = TRUE THEN 1 ELSE 0 END) AS available_rooms,
                    SUM(CASE WHEN available = FALSE THEN 1 ELSE 0 END) AS occupied_rooms
                FROM rooms
                GROUP BY type
                """;
        List<RoomTypeStats> stats = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                stats.add(new RoomTypeStats(
                        RoomType.fromDatabaseValue(resultSet.getString("type")),
                        resultSet.getInt("available_rooms"),
                        resultSet.getInt("occupied_rooms")
                ));
            }
        }
        return stats;
    }

    private void fillRoomStatement(PreparedStatement statement, Room room) throws SQLException {
        statement.setString(1, room.getRoomNumber());
        statement.setString(2, room.getType().getDatabaseValue());
        statement.setBigDecimal(3, room.getPrice());
        statement.setBoolean(4, room.isAvailable());
    }

    private List<Room> mapRooms(ResultSet resultSet) throws SQLException {
        List<Room> rooms = new ArrayList<>();
        while (resultSet.next()) {
            rooms.add(mapRoom(resultSet));
        }
        return rooms;
    }

    private Room mapRoom(ResultSet resultSet) throws SQLException {
        return new Room(
                resultSet.getInt("id"),
                resultSet.getString("room_number"),
                RoomType.fromDatabaseValue(resultSet.getString("type")),
                resultSet.getBigDecimal("price"),
                resultSet.getBoolean("available")
        );
    }
}
