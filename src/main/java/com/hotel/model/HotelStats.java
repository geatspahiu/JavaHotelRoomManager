package com.hotel.model;

public final class HotelStats {
    private final int totalRooms;
    private final int occupiedRooms;
    private final int availableRooms;

    public HotelStats(int totalRooms, int occupiedRooms, int availableRooms) {
        this.totalRooms = totalRooms;
        this.occupiedRooms = occupiedRooms;
        this.availableRooms = availableRooms;
    }

    public int totalRooms() {
        return totalRooms;
    }

    public int occupiedRooms() {
        return occupiedRooms;
    }

    public int availableRooms() {
        return availableRooms;
    }
}
