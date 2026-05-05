package com.hotel.model;

import java.math.BigDecimal;

public class Room {
    private int id;
    private String roomNumber;
    private RoomType type;
    private BigDecimal price;
    private boolean available;

    public Room() {
    }

    public Room(int id, String roomNumber, RoomType type, BigDecimal price, boolean available) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.type = type;
        this.price = price;
        this.available = available;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public RoomType getType() {
        return type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return roomNumber + " - " + type;
    }
}
