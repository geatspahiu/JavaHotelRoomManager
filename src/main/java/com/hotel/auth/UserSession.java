package com.hotel.auth;

import java.time.LocalDateTime;

public record UserSession(UserAccount user, boolean rememberMe, LocalDateTime signedInAt) {
    public boolean isAdmin() {
        return user.getRole() == UserRole.ADMIN;
    }

    public String initials() {
        String[] parts = user.getName().trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
