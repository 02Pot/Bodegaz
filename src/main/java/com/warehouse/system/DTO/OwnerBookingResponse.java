package com.warehouse.system.DTO;

import com.warehouse.system.Enums.BookingStatus;
import com.warehouse.system.Model.Storage;
import com.warehouse.system.Model.StorageBooking;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Model.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OwnerBookingResponse(
        UUID orderId,
        String unitNumber,
        LocalDateTime startDate,
        LocalDateTime endDate,
        BookingStatus status,
        double rentedVolumeCubicMeter,
        double rentedWeightKg,
        BigDecimal totalPrice,
        LocalDateTime requestExpiresAt,
        UUID warehouseId,
        String warehouseName,
        UUID storageId,
        String blockName,
        String section,
        UUID tenantId,
        String tenantName,
        String tenantEmail
) {
    public static OwnerBookingResponse from(StorageBooking b) {
        Storage s = b.getStorage();
        Warehouse w = s.getWarehouse();
        UserModel t = b.getUser();
        return new OwnerBookingResponse(
                b.getOrderId(), b.getUnitNumber(), b.getStartDate(), b.getEndDate(), b.getStatus(),
                b.getRentedVolumeCubicMeter(), b.getRentedWeightKg(),b.getTotalPrice(), b.getHoldExpiresAt(),
                w.getWarehouseId(), w.getName(),
                s.getStorageId(), s.getBlockName(), s.getSection(),
                t.getId(), t.getName(), t.getEmail());
    }
}