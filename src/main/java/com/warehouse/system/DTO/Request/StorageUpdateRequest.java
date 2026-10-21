package com.warehouse.system.DTO.Request;

import com.warehouse.system.Enums.MaterialType;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record StorageUpdateRequest(
        String blockName,
        String section,
        @Positive Double maxWeight,
        @Positive BigDecimal pricePerCubicMeter,
        @Positive Double maxVolumeCubicMeter,
        MaterialType materialType
) {
}
