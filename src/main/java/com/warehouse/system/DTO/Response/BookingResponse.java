package com.warehouse.system.DTO.Response;

import com.warehouse.system.Enums.BookingStatus;
import com.warehouse.system.Model.StorageBooking;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponse(
        UUID orderId,
        UUID storageId,
        String unitNumber,
        LocalDateTime startDate,
        LocalDateTime endDate,
        BookingStatus status,
        LocalDateTime holdExpiresAt,
        Long secondsRemaining
) {
    public static BookingResponse from(StorageBooking b, LocalDateTime now) {
        Long remaining = null;
        if (b.getStatus() == BookingStatus.HELD && b.getHoldExpiresAt() != null) {
            remaining = Math.max(0, Duration.between(now, b.getHoldExpiresAt()).getSeconds());
        }
        return new BookingResponse(
                b.getOrderId(),
                b.getStorage().getStorageId(),
                b.getUnitNumber(),
                b.getStartDate(),
                b.getEndDate(),
                b.getStatus(),
                b.getHoldExpiresAt(),
                remaining);
    }
}
