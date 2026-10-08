package com.warehouse.system.DTO.Response;

import com.warehouse.system.Model.Warehouse;

import java.time.Instant;
import java.util.UUID;

public record WarehouseResponse(
        UUID warehouseId,
        String name,
        double warehouseCapacityKg,
        Long viewCount,
        Instant createdAt,
        UUID userId,
        AddressResponse address,
        boolean saved
) {
    public static WarehouseResponse from(Warehouse w,boolean saved) {
        return new WarehouseResponse(
                w.getWarehouseId(),
                w.getName(),
                w.getWarehouseCapacityKg(),
                w.getViewCount(),
                w.getCreatedAt(),
                w.getUser().getId(),
                AddressResponse.from(w.getWarehouseAddress()),
                saved
        );
    }

    public static WarehouseResponse from(Warehouse w) {
        return from(w, false);
    }

}