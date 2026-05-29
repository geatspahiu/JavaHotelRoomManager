package com.hotel.auth;

import java.time.LocalDateTime;

public class UserAccount {
    private final String id;
    private String name;
    private String email;
    private String password;
    private UserRole role;
    private boolean active;
    private LocalDateTime lastActive;

    public UserAccount(String id, String name, String email, String password, UserRole role, boolean active) {
        this(id, name, email, password, role, active, LocalDateTime.now());
    }

    public UserAccount(String id, String name, String email, String password, UserRole role, boolean active, LocalDateTime lastActive) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.active = active;
        this.lastActive = lastActive == null ? LocalDateTime.now() : lastActive;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getLastActive() {
        return lastActive;
    }

    public void setLastActive(LocalDateTime lastActive) {
        this.lastActive = lastActive;
    }

    public void touch() {
        this.lastActive = LocalDateTime.now();
    }
}
