package com.hotel.service;

import com.hotel.dao.GuestDAO;
import com.hotel.model.Guest;

import java.sql.SQLException;
import java.util.List;

public class GuestService {
    private static final String EMAIL_PATTERN = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$";
    private static final String PHONE_PATTERN = "^[+()0-9 .-]{7,30}$";
    private final GuestDAO guestDAO = new GuestDAO();

    public void save(Guest guest) throws SQLException {
        validate(guest);
        if (guest.getId() == 0) {
            guestDAO.create(guest);
        } else {
            guestDAO.update(guest);
        }
    }

    public void delete(int id) throws SQLException {
        guestDAO.delete(id);
    }

    public List<Guest> findAll() throws SQLException {
        return guestDAO.findAll();
    }

    public List<Guest> search(String query) throws SQLException {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        return guestDAO.search(query);
    }

    private void validate(Guest guest) {
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
}
