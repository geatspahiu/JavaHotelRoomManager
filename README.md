# Java Hotel Room Manager

A desktop Hotel Room Management System built with Java Swing, FlatLaf, JDBC, and MySQL.

## Overview

Java Hotel Room Manager helps manage hotel rooms, guests, and bookings from a clean desktop interface. The app supports room availability tracking, guest management, automatic check-out status syncing, and booking creation with one main guest plus additional guests.

## Features

- Room type dashboard for `Single`, `Double`, and `Suite` rooms.
- Available and occupied room counts by room type.
- Add one room or bulk-create multiple rooms at once.
- Guest CRUD management.
- Search existing guests by name, phone, or email.
- Create a booking and automatically create the main guest.
- Add multiple additional guests to one booking.
- Automatically assign an available room by selected room type.
- Show a clear message when no rooms are available.
- Mark bookings as completed during check-out.
- Auto-complete expired bookings when their check-out date has passed.
- Dark FlatLaf user interface.
- MySQL schema included.

## Tech Stack

- Java 17
- Swing
- FlatLaf
- JDBC
- MySQL
- Maven

## Project Structure

```text
src/main/java/com/hotel
├── config      # Database connection settings
├── dao         # JDBC queries and persistence logic
├── model       # Room, Guest, Booking, and stats entities
├── service     # Business rules and transactions
└── ui          # Swing screens and dialogs

database
└── schema.sql  # MySQL database and table creation script
```

## Database Setup

1. Open MySQL.
2. Run the schema file:

   ```sql
   SOURCE database/schema.sql;
   ```

3. Update your MySQL credentials in:

   ```text
   src/main/java/com/hotel/config/DatabaseConfig.java
   ```

Default config:

```java
public static final String URL = "jdbc:mysql://localhost:3306/hotel_management";
public static final String USER = "root";
public static final String PASSWORD = "";
```

## Run The App

From the project folder:

```bash
mvn compile exec:java
```

## Build Check

```bash
mvn clean compile
```

## Main Screens

- `Rooms`: compact room availability summary by type, plus add and bulk-add actions.
- `Guests`: add, update, delete, and view guests.
- `Bookings`: book by room type, search or create guests, add extra guests, filter by status, and check out.
- `Statistics`: total rooms, occupied rooms, and available rooms.

## Notes

- The booking table displays the main guest as the booking name.
- Additional guests are stored in `booking_guests`.
- Expired active bookings are automatically marked completed before bookings are displayed.
