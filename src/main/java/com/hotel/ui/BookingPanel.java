package com.hotel.ui;

import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;
import com.hotel.model.Guest;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import com.hotel.service.BookingService;
import com.hotel.service.GuestService;
import com.hotel.service.RoomService;

import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BookingPanel extends JPanel {
    private final BookingService bookingService = new BookingService();
    private final RoomService roomService = new RoomService();
    private final GuestService guestService = new GuestService();
    private final BookingTableModel tableModel = new BookingTableModel();
    private final JTable table = new JTable(tableModel);
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All", "In Stay", "Upcoming", "Completed"});

    public BookingPanel() {
        setLayout(new BorderLayout(10, 10));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton bookButton = button("Book Room");
        JButton completeButton = button("Check Out");
        JButton filterButton = button("Filter");
        JButton refreshButton = button("Refresh");

        toolbar.add(bookButton);
        toolbar.add(completeButton);
        toolbar.add(new JLabel("Status"));
        toolbar.add(statusFilter);
        toolbar.add(filterButton);
        toolbar.add(refreshButton);

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        bookButton.addActionListener(event -> openBookingDialog());
        completeButton.addActionListener(event -> completeSelectedBooking());
        filterButton.addActionListener(event -> filterBookings());
        refreshButton.addActionListener(event -> loadData());
    }

    public void loadData() {
        try {
            tableModel.setBookings(bookingService.findAll());
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

    private void filterBookings() {
        try {
            tableModel.setBookings(bookingService.findByStatus(selectedStatus()));
        } catch (SQLException ex) {
            UiUtils.showError(this, ex);
        }
    }

    private BookingStatus selectedStatus() {
        String value = (String) statusFilter.getSelectedItem();
        if ("In Stay".equals(value) || "Upcoming".equals(value)) {
            return BookingStatus.ACTIVE;
        }
        if ("Completed".equals(value)) {
            return BookingStatus.COMPLETED;
        }
        return null;
    }

    private void completeSelectedBooking() {
        Booking booking = tableModel.getBookingAt(table.getSelectedRow());
        if (booking == null || !UiUtils.confirm(this, "Mark selected booking as completed?")) {
            return;
        }
        try {
            bookingService.completeBooking(booking.getId());
            loadData();
        } catch (Exception ex) {
            UiUtils.showError(this, ex);
        }
    }

    private void openBookingDialog() {
        try {
            List<Room> availableRooms = roomService.findAvailableRooms();
            if (availableRooms.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No available rooms.", "Booking",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Set<RoomType> availableTypes = new LinkedHashSet<>();
            for (Room room : availableRooms) {
                availableTypes.add(room.getType());
            }
            JComboBox<RoomType> roomTypeBox = new JComboBox<>(availableTypes.toArray(new RoomType[0]));
            JTextField guestSearchField = new JTextField();
            DefaultListModel<Guest> guestListModel = new DefaultListModel<>();
            JList<Guest> guestList = new JList<>(guestListModel);
            guestList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            guestList.setVisibleRowCount(5);
            JButton searchGuestButton = button("Search");
            JButton newGuestButton = button("New Guest");
            JTextField guestNameField = new JTextField();
            JTextField guestPhoneField = new JTextField();
            JTextField guestEmailField = new JTextField();
            DefaultListModel<Guest> additionalGuestModel = new DefaultListModel<>();
            JList<Guest> additionalGuestList = new JList<>(additionalGuestModel);
            additionalGuestList.setVisibleRowCount(4);
            JTextField additionalGuestNameField = new JTextField();
            JTextField additionalGuestPhoneField = new JTextField();
            JTextField additionalGuestEmailField = new JTextField();
            JButton addAdditionalGuestButton = button("Add Guest");
            JButton removeAdditionalGuestButton = button("Remove");
            JTextField checkInField = new JTextField(LocalDate.now().toString());
            JTextField checkOutField = new JTextField(LocalDate.now().plusDays(1).toString());

            Runnable loadGuests = () -> {
                try {
                    guestListModel.clear();
                    for (Guest guest : guestService.search(guestSearchField.getText())) {
                        guestListModel.addElement(guest);
                    }
                } catch (SQLException ex) {
                    UiUtils.showError(this, ex);
                }
            };
            loadGuests.run();

            searchGuestButton.addActionListener(event -> loadGuests.run());
            newGuestButton.addActionListener(event -> {
                guestList.clearSelection();
                guestNameField.setText("");
                guestPhoneField.setText("");
                guestEmailField.setText("");
            });
            guestList.addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting() && guestList.getSelectedValue() != null) {
                    Guest guest = guestList.getSelectedValue();
                    guestNameField.setText(guest.getName());
                    guestPhoneField.setText(guest.getPhone());
                    guestEmailField.setText(guest.getEmail());
                }
            });
            addAdditionalGuestButton.addActionListener(event -> {
                try {
                    Guest guest = new Guest();
                    guest.setName(additionalGuestNameField.getText().trim());
                    guest.setPhone(additionalGuestPhoneField.getText().trim());
                    guest.setEmail(additionalGuestEmailField.getText().trim());
                    if (guest.getName().isBlank() || guest.getPhone().isBlank() || guest.getEmail().isBlank()) {
                        throw new IllegalArgumentException("Additional guest name, phone, and email are required.");
                    }
                    additionalGuestModel.addElement(guest);
                    additionalGuestNameField.setText("");
                    additionalGuestPhoneField.setText("");
                    additionalGuestEmailField.setText("");
                } catch (Exception ex) {
                    UiUtils.showError(this, ex);
                }
            });
            removeAdditionalGuestButton.addActionListener(event -> {
                int selectedIndex = additionalGuestList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    additionalGuestModel.remove(selectedIndex);
                }
            });

            JPanel guestSearchPanel = new JPanel(new BorderLayout(8, 8));
            JPanel guestSearchActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            guestSearchActions.add(searchGuestButton);
            guestSearchActions.add(newGuestButton);
            guestSearchPanel.add(guestSearchField, BorderLayout.CENTER);
            guestSearchPanel.add(guestSearchActions, BorderLayout.EAST);

            JScrollPane guestListPane = new JScrollPane(guestList);
            guestListPane.setPreferredSize(new Dimension(360, 110));
            JScrollPane additionalGuestPane = new JScrollPane(additionalGuestList);
            additionalGuestPane.setPreferredSize(new Dimension(360, 85));
            JPanel additionalGuestActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            additionalGuestActions.add(addAdditionalGuestButton);
            additionalGuestActions.add(removeAdditionalGuestButton);
            JPanel additionalGuestFields = new JPanel(new GridBagLayout());
            addCompactFormRow(additionalGuestFields, 0, "Name", additionalGuestNameField);
            addCompactFormRow(additionalGuestFields, 1, "Phone", additionalGuestPhoneField);
            addCompactFormRow(additionalGuestFields, 2, "Email", additionalGuestEmailField);
            JPanel additionalGuestPanel = new JPanel(new BorderLayout(8, 8));
            additionalGuestPanel.add(additionalGuestFields, BorderLayout.CENTER);
            additionalGuestPanel.add(additionalGuestActions, BorderLayout.NORTH);
            additionalGuestPanel.add(additionalGuestPane, BorderLayout.SOUTH);

            JPanel form = new JPanel(new GridBagLayout());
            addFormRow(form, 0, "Room type", roomTypeBox);
            addFormRow(form, 1, "Search existing guest", guestSearchPanel);
            addFormRow(form, 2, "Matching guests", guestListPane);
            addFormRow(form, 3, "Main guest name", guestNameField);
            addFormRow(form, 4, "Main guest phone", guestPhoneField);
            addFormRow(form, 5, "Main guest email", guestEmailField);
            addFormRow(form, 6, "Additional guests", additionalGuestPanel);
            addFormRow(form, 7, "Check-in date (yyyy-mm-dd)", checkInField);
            addFormRow(form, 8, "Check-out date (yyyy-mm-dd)", checkOutField);

            int result = JOptionPane.showConfirmDialog(this, form, "Book Room",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            RoomType selectedRoomType = (RoomType) roomTypeBox.getSelectedItem();
            Guest selectedGuest = guestList.getSelectedValue();
            if (selectedRoomType == null) {
                throw new IllegalArgumentException("Room type is required.");
            }
            Room selectedRoom = findAvailableRoomByType(availableRooms, selectedRoomType);

            Booking booking = new Booking();
            booking.setRoomId(selectedRoom.getId());
            booking.setCheckIn(LocalDate.parse(checkInField.getText().trim()));
            booking.setCheckOut(LocalDate.parse(checkOutField.getText().trim()));
            Guest bookingGuest = selectedGuest == null ? new Guest() : selectedGuest;
            bookingGuest.setName(guestNameField.getText().trim());
            bookingGuest.setPhone(guestPhoneField.getText().trim());
            bookingGuest.setEmail(guestEmailField.getText().trim());
            bookingService.bookRoomWithGuests(booking, bookingGuest, guestsFromModel(additionalGuestModel));
            loadData();
        } catch (DateTimeParseException ex) {
            UiUtils.showError(this, new IllegalArgumentException("Dates must use yyyy-mm-dd format."));
        } catch (Exception ex) {
            UiUtils.showError(this, ex);
        }
    }

    private List<Guest> guestsFromModel(DefaultListModel<Guest> model) {
        List<Guest> guests = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            guests.add(model.getElementAt(i));
        }
        return guests;
    }

    private Room findAvailableRoomByType(List<Room> availableRooms, RoomType roomType) {
        for (Room room : availableRooms) {
            if (room.getType() == roomType) {
                return room;
            }
        }
        throw new IllegalArgumentException("No available rooms for selected type.");
    }

    private void addFormRow(JPanel form, int row, String labelText, java.awt.Component field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(5, 5, 5, 12);
        form.add(new JLabel(labelText), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(5, 5, 5, 5);
        form.add(field, fieldConstraints);
    }

    private void addCompactFormRow(JPanel form, int row, String labelText, java.awt.Component field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(2, 0, 2, 8);
        form.add(new JLabel(labelText), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(2, 0, 2, 0);
        form.add(field, fieldConstraints);
    }

    private static class BookingTableModel extends AbstractTableModel {
        private final String[] columns = {"ID", "Room", "Guest", "Check In", "Check Out", "Status"};
        private List<Booking> bookings = new ArrayList<>();

        void setBookings(List<Booking> bookings) {
            this.bookings = bookings;
            fireTableDataChanged();
        }

        Booking getBookingAt(int row) {
            if (row < 0 || row >= bookings.size()) {
                return null;
            }
            return bookings.get(row);
        }

        @Override
        public int getRowCount() {
            return bookings.size();
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
            Booking booking = bookings.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> booking.getId();
                case 1 -> booking.getRoomNumber();
                case 2 -> booking.getGuestName();
                case 3 -> booking.getCheckIn();
                case 4 -> booking.getCheckOut();
                case 5 -> displayStatus(booking);
                default -> "";
            };
        }

        private String displayStatus(Booking booking) {
            if (booking.getStatus() == BookingStatus.COMPLETED) {
                return "Completed";
            }
            LocalDate today = LocalDate.now();
            if (booking.getCheckIn().isAfter(today)) {
                return "Upcoming";
            }
            if (!booking.getCheckOut().isAfter(today)) {
                return "Completed";
            }
            return "In Stay";
        }
    }
}
