package com.hotel.ui;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.sql.SQLException;

final class UiUtils {
    private UiUtils() {
    }

    static void showError(Component parent, Exception ex) {
        String message = ex instanceof SQLException
                ? "Database error: " + ex.getMessage()
                : ex.getMessage();
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
