package com.hotel.auth;

import com.hotel.dao.UserAccountDAO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AuthService {
    private static final String EMAIL_PATTERN = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$";
    private final UserAccountDAO userAccountDAO = new UserAccountDAO();

    public Optional<UserSession> login(String emailOrUsername, String password, boolean rememberMe) {
        String normalized = emailOrUsername == null ? "" : emailOrUsername.trim().toLowerCase();
        try {
            Optional<UserAccount> account = userAccountDAO.findLoginCandidate(normalized)
                    .filter(user -> user.getPassword().equals(password));
            if (account.isEmpty()) {
                return Optional.empty();
            }
            UserAccount user = account.get();
            user.touch();
            userAccountDAO.updateLastActive(user);
            return Optional.of(new UserSession(user, rememberMe, LocalDateTime.now()));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to access user accounts: " + ex.getMessage(), ex);
        }
    }

    public List<UserAccount> users() {
        try {
            return userAccountDAO.findAll();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load workers: " + ex.getMessage(), ex);
        }
    }

    public void saveWorker(UserAccount worker) {
        if (worker.getName() == null || worker.getName().isBlank()) {
            throw new IllegalArgumentException("Worker name is required.");
        }
        if (worker.getEmail() == null || worker.getEmail().isBlank()) {
            throw new IllegalArgumentException("Worker email is required.");
        }
        if (!worker.getEmail().trim().toUpperCase().matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Worker email format is invalid.");
        }
        if (worker.getPassword() == null || worker.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (worker.getRole() == null) {
            throw new IllegalArgumentException("Worker role is required.");
        }
        String normalizedEmail = worker.getEmail().trim().toLowerCase();
        boolean duplicateEmail = users().stream()
                .anyMatch(user -> !user.getId().equals(worker.getId()) && user.getEmail().equalsIgnoreCase(normalizedEmail));
        if (duplicateEmail) {
            throw new IllegalArgumentException("A worker with this email already exists.");
        }

        String normalizedName = worker.getName().trim();
        UserAccount saved = new UserAccount(worker.getId(), normalizedName, normalizedEmail,
                worker.getPassword(), worker.getRole(), worker.isActive(), worker.getLastActive());
        try {
            userAccountDAO.save(saved);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to save worker: " + ex.getMessage(), ex);
        }
    }

    public void removeWorker(String id) {
        try {
            userAccountDAO.removeWorker(id);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to remove worker: " + ex.getMessage(), ex);
        }
    }
}
