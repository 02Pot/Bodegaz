package com.warehouse.system.DTO.Response;

import com.warehouse.system.Model.Warehouse;

import java.util.UUID;

public record WarehouseResponse(
        UUID warehouseId, String name, double warehouseCapacityKg,
        UUID userId, UUID addressId) {

    public static WarehouseResponse from(Warehouse w) {
        return new WarehouseResponse(
                w.getWarehouseId(),
                w.getName(),
                w.getWarehouseCapacityKg(),
                w.getUser().getId(),
                w.getWarehouseAddress().getAddressId());
    }


}