package com.warehouse.system.DTO.Interface;

import com.warehouse.system.DTO.Response.AddressResponse;

import java.util.List;
import java.util.UUID;

public record WarehouseSummary(
        UUID warehouseId,
        String name,
        double warehouseCapacityKg,
        AddressResponse address,
        List<String> imageUrls
) {
}
