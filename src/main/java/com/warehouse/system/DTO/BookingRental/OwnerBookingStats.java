package com.warehouse.system.DTO.BookingRental;

import java.math.BigDecimal;

public record OwnerBookingStats(long active, long pending, long total, long endingSoon, BigDecimal revenue) {
    public OwnerBookingStats {
        if (revenue == null) revenue = BigDecimal.ZERO;
    }
}