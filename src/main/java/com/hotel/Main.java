package com.hotel;

import com.formdev.flatlaf.FlatDarkLaf;
import com.hotel.config.AppBrand;
import com.hotel.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", AppBrand.NAME);
        System.setProperty("apple.awt.application.appearance", "system");
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("sun.java2d.opengl", "true");

        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            UIManager.put("TitlePane.unifiedBackground", true);
            UIManager.put("TitlePane.buttonSize", 20);
            UIManager.put("Component.arc", 14);
            UIManager.put("Button.arc", 14);
            UIManager.put("TextComponent.arc", 14);
            UIManager.put("ComboBox.arc", 14);
            UIManager.put("ScrollBar.width", 11);
            UIManager.put("Table.rowHeight", 46);
            UIManager.put("defaultFont", new java.awt.Font("Inter", java.awt.Font.PLAIN, 13));
            new LoginFrame().setVisible(true);
        });
    }
}
