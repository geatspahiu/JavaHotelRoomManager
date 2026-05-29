package com.hotel.ui;

import com.hotel.model.HotelStats;
import com.hotel.service.RoomService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;

public class StatsPanel extends JPanel {
    private final RoomService roomService = new RoomService();
    private final JLabel totalRoomsValue = new JLabel("0");
    private final JLabel occupiedRoomsValue = new JLabel("0");
    private final JLabel availableRoomsValue = new JLabel("0");

    public StatsPanel() {
        setLayout(new BorderLayout(10, 10));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = button("Refresh");
        toolbar.add(refreshButton);

        JPanel stats = new JPanel(new GridLayout(0, 2, 12, 12));
        stats.add(label("Total Rooms"));
        stats.add(value(totalRoomsValue));
        stats.add(label("Occupied Rooms"));
        stats.add(value(occupiedRoomsValue));
        stats.add(label("Available Rooms"));
        stats.add(value(availableRoomsValue));

        add(toolbar, BorderLayout.NORTH);
        add(stats, BorderLayout.CENTER);

        refreshButton.addActionListener(event -> loadStats());
    }

    public void loadStats() {
        try {
            HotelStats stats = roomService.getStats();
            totalRoomsValue.setText(String.valueOf(stats.totalRooms()));
            occupiedRoomsValue.setText(String.valueOf(stats.occupiedRooms()));
            availableRoomsValue.setText(String.valueOf(stats.availableRooms()));
        } catch (SQLException ex) {
            UiUtils.showError(this, ex);
        }
    }

    private static JButton button(String text) {
        JButton button = new JButton(text);
        button.putClientProperty("JButton.arc", 14);
        button.setFocusPainted(false);
        return button;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        return label;
    }

    private JLabel value(JLabel label) {
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 18f));
        return label;
    }
}
