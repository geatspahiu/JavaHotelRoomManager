package com.hotel.model;

public enum RoomType {
    SINGLE("Single"),
    DOUBLE("Double"),
    SUITE("Suite");

    private final String databaseValue;

    RoomType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    @Override
    public String toString() {
        return databaseValue;
    }

    public static RoomType fromDatabaseValue(String value) {
        for (RoomType type : values()) {
            if (type.databaseValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown room type: " + value);
    }
}
