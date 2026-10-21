package com.warehouse.system.DTO.Request;

import com.warehouse.system.Enums.MaterialType;
import com.warehouse.system.Model.StorageType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record StorageRequest(
        @NotBlank String blockName,
        String section,
        @Positive Double maxWeight,
        @NotNull MaterialType materialType,
        @NotNull UUID storageTypeId,
        @Positive BigDecimal pricePerCubicMeter,
        @Positive double maxVolumeCubicMeter,
        @Min(1) @Max(100) Integer quantity
) {
}
