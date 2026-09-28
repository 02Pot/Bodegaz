package com.warehouse.system.DTO.Request;

import com.warehouse.system.Enums.MaterialType;
import com.warehouse.system.Model.StorageType;

public record StorageRequest(
        String blockName,
        String section,
        double maxWeight,
        MaterialType materialType,
        StorageType storageType,
        double lengthMeters,
        double breadthMeters,
        double heightMeters,
        double capacityWeight,
        double unitsAvailable
) {
}
