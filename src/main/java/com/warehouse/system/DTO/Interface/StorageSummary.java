package com.warehouse.system.DTO.Interface;

import com.warehouse.system.Enums.MaterialType;

import java.math.BigDecimal;
import java.util.UUID;

public record StorageSummary(
        UUID storageId,
        String blockName,
        String section,
        double maxWeight,
        BigDecimal pricePerCubicMeter,
        double maxVolumeCubicMeter,
        MaterialType materialType
) {
}
