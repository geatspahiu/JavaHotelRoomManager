package com.hotel.dao;

import com.hotel.auth.UserAccount;
import com.hotel.auth.UserRole;
import com.hotel.config.AppBrand;
import com.hotel.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserAccountDAO {
    public List<UserAccount> findAll() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            ensureTable(connection);
            String sql = "SELECT * FROM user_accounts ORDER BY role, name";
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {
                return mapUsers(resultSet);
            }
        }
    }

    public Optional<UserAccount> findLoginCandidate(String emailOrUsername) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            ensureTable(connection);
            String sql = """
                    SELECT * FROM user_accounts
                    WHERE active = TRUE
                      AND (LOWER(email) = ? OR LOWER(REPLACE(name, ' ', '')) = ?)
                    LIMIT 1
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, emailOrUsername);
                statement.setString(2, emailOrUsername);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapUser(resultSet));
                    }
                    return Optional.empty();
                }
            }
        }
    }

    public void save(UserAccount user) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            ensureTable(connection);
            if (user.getId() == null || user.getId().isBlank() || !exists(connection, user.getId())) {
                insert(connection, user);
            } else {
                update(connection, user);
            }
        }
    }

    public void updateLastActive(UserAccount user) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            ensureTable(connection);
            String sql = "UPDATE user_accounts SET last_active = ? WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setTimestamp(1, Timestamp.valueOf(user.getLastActive()));
                statement.setString(2, user.getId());
                statement.executeUpdate();
            }
        }
    }

    public void removeWorker(String id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            ensureTable(connection);
            String sql = "DELETE FROM user_accounts WHERE id = ? AND role <> ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, id);
                statement.setString(2, UserRole.ADMIN.name());
                statement.executeUpdate();
            }
        }
    }

    private void ensureTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS user_accounts (
                    id VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(120) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    role ENUM('ADMIN', 'WORKER') NOT NULL,
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    last_active DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
        seedDefaults(connection);
    }

    private void seedDefaults(Connection connection) throws SQLException {
        String sql = """
                INSERT IGNORE INTO user_accounts (id, name, email, password, role, active, last_active)
                VALUES (?, ?, ?, ?, ?, TRUE, ?), (?, ?, ?, ?, ?, TRUE, ?)
                """;
        LocalDateTime now = LocalDateTime.now();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "admin-1");
            statement.setString(2, "Hotel Admin");
            statement.setString(3, AppBrand.ADMIN_EMAIL);
            statement.setString(4, "admin123");
            statement.setString(5, UserRole.ADMIN.name());
            statement.setTimestamp(6, Timestamp.valueOf(now));
            statement.setString(7, "worker-1");
            statement.setString(8, "Front Desk");
            statement.setString(9, AppBrand.WORKER_EMAIL);
            statement.setString(10, "worker123");
            statement.setString(11, UserRole.WORKER.name());
            statement.setTimestamp(12, Timestamp.valueOf(now));
            statement.executeUpdate();
        }
    }

    private boolean exists(Connection connection, String id) throws SQLException {
        String sql = "SELECT 1 FROM user_accounts WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void insert(Connection connection, UserAccount user) throws SQLException {
        String sql = """
                INSERT INTO user_accounts (id, name, email, password, role, active, last_active)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            fillStatement(statement, user.getId() == null || user.getId().isBlank() ? UUID.randomUUID().toString() : user.getId(), user);
            statement.executeUpdate();
        }
    }

    private void update(Connection connection, UserAccount user) throws SQLException {
        String sql = """
                UPDATE user_accounts
                SET name = ?, email = ?, password = ?, role = ?, active = ?, last_active = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getRole().name());
            statement.setBoolean(5, user.isActive());
            statement.setTimestamp(6, Timestamp.valueOf(user.getLastActive()));
            statement.setString(7, user.getId());
            statement.executeUpdate();
        }
    }

    private void fillStatement(PreparedStatement statement, String id, UserAccount user) throws SQLException {
        statement.setString(1, id);
        statement.setString(2, user.getName());
        statement.setString(3, user.getEmail());
        statement.setString(4, user.getPassword());
        statement.setString(5, user.getRole().name());
        statement.setBoolean(6, user.isActive());
        statement.setTimestamp(7, Timestamp.valueOf(user.getLastActive()));
    }

    private List<UserAccount> mapUsers(ResultSet resultSet) throws SQLException {
        List<UserAccount> users = new ArrayList<>();
        while (resultSet.next()) {
            users.add(mapUser(resultSet));
        }
        return users;
    }

    private UserAccount mapUser(ResultSet resultSet) throws SQLException {
        return new UserAccount(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getString("password"),
                UserRole.valueOf(resultSet.getString("role")),
                resultSet.getBoolean("active"),
                resultSet.getTimestamp("last_active").toLocalDateTime()
        );
    }
}
