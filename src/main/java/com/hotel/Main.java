package com.hotel;

import com.formdev.flatlaf.FlatDarkLaf;
import com.hotel.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            UIManager.put("Component.arc", 8);
            UIManager.put("Button.arc", 8);
            new MainFrame().setVisible(true);
        });
    }
}
