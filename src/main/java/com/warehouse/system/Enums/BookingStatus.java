package com.warehouse.system.Enums;

public enum BookingStatus {
    PENDING_TERMS,
    PENDING_OWNER,
    PENDING_PAYMENT,
    REJECTED,
    HELD,       // temporary hold, expires at holdExpiresAt
    CONFIRMED,  // paid / final
    CANCELLED,
    EXPIRED,    // hold timed out
}
