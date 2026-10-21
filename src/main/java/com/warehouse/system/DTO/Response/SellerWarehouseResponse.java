package com.warehouse.system.DTO.Response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SellerWarehouseResponse(
        UUID warehouseId,
        String name,
        double warehouseCapacityKg,
        Long viewCount,
        Instant createdAt,
        AddressResponse address,
        double allocatedKg,
        long storageCount,
        long bookedCount,
        boolean draft,
        List<WarehouseImageResponse> images
) {}