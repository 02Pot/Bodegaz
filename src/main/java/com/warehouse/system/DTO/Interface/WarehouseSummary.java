package com.warehouse.system.DTO.Interface;

import com.warehouse.system.DTO.Response.AddressResponse;

import java.util.List;

public record WarehouseSummary(
        String name,
        double warehouseCapacityKg,
        AddressResponse address,
        List<String> imageUrls
) {
}
