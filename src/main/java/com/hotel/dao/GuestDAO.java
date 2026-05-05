package com.hotel.dao;

import com.hotel.config.DatabaseConnection;
import com.hotel.model.Guest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GuestDAO {
    public void create(Guest guest) throws SQLException {
        String sql = "INSERT INTO guests (name, phone, email) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillGuestStatement(statement, guest);
            statement.executeUpdate();
        }
    }

    public int create(Connection connection, Guest guest) throws SQLException {
        String sql = "INSERT INTO guests (name, phone, email) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillGuestStatement(statement, guest);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Creating guest failed, no ID returned.");
            }
        }
    }

    public void update(Guest guest) throws SQLException {
        String sql = "UPDATE guests SET name = ?, phone = ?, email = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillGuestStatement(statement, guest);
            statement.setInt(4, guest.getId());
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM guests WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public List<Guest> findAll() throws SQLException {
        String sql = "SELECT * FROM guests ORDER BY name";
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return mapGuests(resultSet);
        }
    }

    public List<Guest> search(String query) throws SQLException {
        String sql = """
                SELECT * FROM guests
                WHERE LOWER(name) LIKE LOWER(?)
                   OR LOWER(phone) LIKE LOWER(?)
                   OR LOWER(email) LIKE LOWER(?)
                ORDER BY name
                """;
        String pattern = "%" + query.trim() + "%";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapGuests(resultSet);
            }
        }
    }

    public List<Guest> findByIds(List<Integer> guestIds) throws SQLException {
        if (guestIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(guestIds.size(), "?"));
        String sql = "SELECT * FROM guests WHERE id IN (" + placeholders + ") ORDER BY name";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < guestIds.size(); i++) {
                statement.setInt(i + 1, guestIds.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapGuests(resultSet);
            }
        }
    }

    private List<Guest> mapGuests(ResultSet resultSet) throws SQLException {
            List<Guest> guests = new ArrayList<>();
            while (resultSet.next()) {
                guests.add(new Guest(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getString("phone"),
                        resultSet.getString("email")
                ));
            }
            return guests;
    }

    private void fillGuestStatement(PreparedStatement statement, Guest guest) throws SQLException {
        statement.setString(1, guest.getName());
        statement.setString(2, guest.getPhone());
        statement.setString(3, guest.getEmail());
    }
}
