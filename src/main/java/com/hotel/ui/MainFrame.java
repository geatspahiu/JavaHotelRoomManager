package com.hotel.ui;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

public class MainFrame extends JFrame {
    private final RoomPanel roomPanel = new RoomPanel();
    private final GuestPanel guestPanel = new GuestPanel();
    private final BookingPanel bookingPanel = new BookingPanel();
    private final StatsPanel statsPanel = new StatsPanel();

    public MainFrame() {
        super("Hotel Room Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1050, 680);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Rooms", roomPanel);
        tabs.addTab("Guests", guestPanel);
        tabs.addTab("Bookings", bookingPanel);
        tabs.addTab("Statistics", statsPanel);
        tabs.addChangeListener(event -> refreshCurrentTab(tabs.getSelectedIndex()));

        add(tabs, BorderLayout.CENTER);
        refreshAll();
    }

    private void refreshCurrentTab(int index) {
        switch (index) {
            case 0 -> roomPanel.loadRooms();
            case 1 -> guestPanel.loadGuests();
            case 2 -> bookingPanel.loadData();
            case 3 -> statsPanel.loadStats();
            default -> {
            }
        }
    }

    private void refreshAll() {
        roomPanel.loadRooms();
        guestPanel.loadGuests();
        bookingPanel.loadData();
        statsPanel.loadStats();
    }
}
