package com.warehouse.system.Enums;

public enum BookingStatus {
    HELD,       // temporary hold, expires at holdExpiresAt
    CONFIRMED,  // paid / final
    CANCELLED,
    EXPIRED,    // hold timed out
    COMPLETED
}
