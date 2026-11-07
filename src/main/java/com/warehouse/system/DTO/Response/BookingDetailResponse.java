package com.warehouse.system.DTO.Response;

import com.warehouse.system.DTO.Interface.StorageSummary;
import com.warehouse.system.DTO.Interface.WarehouseSummary;
import com.warehouse.system.Enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingDetailResponse(
        UUID orderId,
        String unitNumber,
        LocalDateTime startDate,
        LocalDateTime endDate,
        BookingStatus status,
        StorageSummary storage,
        WarehouseSummary warehouse
) {
}
