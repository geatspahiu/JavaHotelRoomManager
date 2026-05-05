package com.hotel.ui;

import com.hotel.model.Guest;
import com.hotel.service.GuestService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GuestPanel extends JPanel {
    private final GuestService guestService = new GuestService();
    private final GuestTableModel tableModel = new GuestTableModel();
    private final JTable table = new JTable(tableModel);

    public GuestPanel() {
        setLayout(new BorderLayout(10, 10));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        JButton refreshButton = new JButton("Refresh");
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);
        toolbar.add(refreshButton);

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        addButton.addActionListener(event -> openGuestDialog(null));
        editButton.addActionListener(event -> editSelectedGuest());
        deleteButton.addActionListener(event -> deleteSelectedGuest());
        refreshButton.addActionListener(event -> loadGuests());
    }

    public void loadGuests() {
        try {
            tableModel.setGuests(guestService.findAll());
        } catch (SQLException ex) {
            UiUtils.showError(this, ex);
        }
    }

    private void editSelectedGuest() {
        Guest guest = tableModel.getGuestAt(table.getSelectedRow());
        if (guest != null) {
            openGuestDialog(guest);
        }
    }

    private void deleteSelectedGuest() {
        Guest guest = tableModel.getGuestAt(table.getSelectedRow());
        if (guest == null || !UiUtils.confirm(this, "Delete selected guest?")) {
            return;
        }
        try {
            guestService.delete(guest.getId());
            loadGuests();
        } catch (Exception ex) {
            UiUtils.showError(this, ex);
        }
    }

    private void openGuestDialog(Guest guest) {
        JTextField nameField = new JTextField(guest == null ? "" : guest.getName());
        JTextField phoneField = new JTextField(guest == null ? "" : guest.getPhone());
        JTextField emailField = new JTextField(guest == null ? "" : guest.getEmail());

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Name"));
        form.add(nameField);
        form.add(new JLabel("Phone"));
        form.add(phoneField);
        form.add(new JLabel("Email"));
        form.add(emailField);

        int result = JOptionPane.showConfirmDialog(this, form,
                guest == null ? "Add Guest" : "Update Guest",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            Guest saved = guest == null ? new Guest() : guest;
            saved.setName(nameField.getText().trim());
            saved.setPhone(phoneField.getText().trim());
            saved.setEmail(emailField.getText().trim());
            guestService.save(saved);
            loadGuests();
        } catch (Exception ex) {
            UiUtils.showError(this, ex);
        }
    }

    private static class GuestTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Name", "Phone", "Email"};
        private List<Guest> guests = new ArrayList<>();

        void setGuests(List<Guest> guests) {
            this.guests = guests;
            fireTableDataChanged();
        }

        Guest getGuestAt(int row) {
            if (row < 0 || row >= guests.size()) {
                return null;
            }
            return guests.get(row);
        }

        @Override
        public int getRowCount() {
            return guests.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Guest guest = guests.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> guest.getId();
                case 1 -> guest.getName();
                case 2 -> guest.getPhone();
                case 3 -> guest.getEmail();
                default -> "";
            };
        }
    }
}
