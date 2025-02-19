package com.irctc2.booking.entity;

public enum BookingStatus {
    PENDING,        // Booking initiated but payment not completed
    CONFIRMED,      // Payment successful, ticket confirmed
    CANCELLED,      // User cancelled the booking
    EXPIRED,        // Travel date passed without check-in
    COMPLETED,      // Journey completed successfully
    REFUNDED,       // Refund processed after cancellation
    NO_SHOW,        // Passenger didn't board the train
    PARTIALLY_USED  // Used for multi-segment journeys where some parts are completed
}
