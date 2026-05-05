package com.hotel.model;

import java.time.LocalDate;

public class Booking {
    private int id;
    private int roomId;
    private int guestId;
    private String roomNumber;
    private String guestName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private BookingStatus status;

    public Booking() {
    }

    public Booking(int id, int roomId, int guestId, String roomNumber, String guestName,
                   LocalDate checkIn, LocalDate checkOut, BookingStatus status) {
        this.id = id;
        this.roomId = roomId;
        this.guestId = guestId;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getGuestId() {
        return guestId;
    }

    public void setGuestId(int guestId) {
        this.guestId = guestId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}
