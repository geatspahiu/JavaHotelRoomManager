package com.hotel.ui;

import com.hotel.model.Room;
import com.hotel.model.RoomType;
import com.hotel.model.RoomTypeStats;
import com.hotel.service.RoomService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class RoomPanel extends JPanel {
    private final RoomService roomService = new RoomService();
    private final Map<RoomType, JLabel> availableLabels = new EnumMap<>(RoomType.class);
    private final Map<RoomType, JLabel> occupiedLabels = new EnumMap<>(RoomType.class);

    public RoomPanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton addButton = button("Add Room");
        JButton bulkAddButton = button("Bulk Add");
        JButton refreshButton = button("Refresh");
        toolbar.add(addButton);
        toolbar.add(bulkAddButton);
        toolbar.add(refreshButton);

        JPanel summaryPanel = new JPanel(new GridLayout(1, RoomType.values().length, 12, 0));
        for (RoomType type : RoomType.values()) {
            summaryPanel.add(createSummaryPanel(type));
        }

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.add(toolbar, BorderLayout.NORTH);
        content.add(summaryPanel, BorderLayout.CENTER);

        add(content, BorderLayout.NORTH);

        addButton.addActionListener(event -> openRoomDialog());
        bulkAddButton.addActionListener(event -> openBulkRoomDialog());
        refreshButton.addActionListener(event -> loadRooms());
    }

    public void loadRooms() {
        try {
            setAllStatsToZero();
            List<RoomTypeStats> stats = roomService.getStatsByType();
            for (RoomTypeStats item : stats) {
                availableLabels.get(item.type()).setText(String.valueOf(item.availableRooms()));
                occupiedLabels.get(item.type()).setText(String.valueOf(item.occupiedRooms()));
            }
        } catch (SQLException ex) {
            UiUtils.showError(this, ex);
        }
    }

    private JPanel createSummaryPanel(RoomType type) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(type.toString()),
                BorderFactory.createEmptyBorder(10, 12, 12, 12)
        ));

        JLabel availableValue = valueLabel();
        JLabel occupiedValue = valueLabel();
        availableLabels.put(type, availableValue);
        occupiedLabels.put(type, occupiedValue);

        panel.add(new JLabel("Available"));
        panel.add(availableValue);
        panel.add(new JLabel("Occupied"));
        panel.add(occupiedValue);
        return panel;
    }

    private JLabel valueLabel() {
        JLabel label = new JLabel("0");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 20f));
        return label;
    }

    private static JButton button(String text) {
        JButton button = new JButton(text);
        button.putClientProperty("JButton.arc", 14);
        button.setFocusPainted(false);
        return button;
    }

    private void setAllStatsToZero() {
        for (RoomType type : RoomType.values()) {
            availableLabels.get(type).setText("0");
            occupiedLabels.get(type).setText("0");
        }
    }

    private void openRoomDialog() {
        JTextField numberField = new JTextField();
        JComboBox<RoomType> typeField = new JComboBox<>(RoomType.values());
        JTextField priceField = new JTextField();
        JCheckBox availableField = new JCheckBox("Available", true);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Room number"));
        form.add(numberField);
        form.add(new JLabel("Type"));
        form.add(typeField);
        form.add(new JLabel("Price"));
        form.add(priceField);
        form.add(new JLabel("Status"));
        form.add(availableField);

        int result = JOptionPane.showConfirmDialog(this, form, "Add Room",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            Room room = new Room();
            room.setRoomNumber(numberField.getText().trim());
            room.setType((RoomType) typeField.getSelectedItem());
            room.setPrice(new BigDecimal(priceField.getText().trim()));
            room.setAvailable(availableField.isSelected());
            roomService.save(room);
            loadRooms();
        } catch (Exception ex) {
            UiUtils.showError(this, ex);
        }
    }

    private void openBulkRoomDialog() {
        JTextField startField = new JTextField("101");
        JTextField countField = new JTextField("10");
        JComboBox<RoomType> typeField = new JComboBox<>(RoomType.values());
        JTextField priceField = new JTextField("50.00");
        JCheckBox availableField = new JCheckBox("Available", true);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Starting room number"));
        form.add(startField);
        form.add(new JLabel("How many rooms"));
        form.add(countField);
        form.add(new JLabel("Type"));
        form.add(typeField);
        form.add(new JLabel("Price"));
        form.add(priceField);
        form.add(new JLabel("Initial status"));
        form.add(availableField);

        int result = JOptionPane.showConfirmDialog(this, form, "Bulk Add Rooms",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            roomService.createRoomRange(
                    startField.getText().trim(),
                    Integer.parseInt(countField.getText().trim()),
                    (RoomType) typeField.getSelectedItem(),
                    new BigDecimal(priceField.getText().trim()),
                    availableField.isSelected()
            );
            loadRooms();
        } catch (Exception ex) {
            UiUtils.showError(this, ex);
        }
    }
}
