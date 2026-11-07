package com.warehouse.system.DTO.Response;

import com.warehouse.system.Enums.MaterialType;
import com.warehouse.system.Model.Storage;

import java.math.BigDecimal;
import java.util.UUID;

public record StorageResponse(
        UUID storageId,
        String blockName,
        String section,
        double maxWeight,
        boolean isListed,
        BigDecimal pricePerCubicMeter,
        double maxVolumeCubicMeter,
        MaterialType materialType,
        UUID warehouseId
) {

    public static StorageResponse from(Storage s) {
        return new StorageResponse(
                s.getStorageId(),
                s.getBlockName(),
                s.getSection(),
                s.getMaxWeight(),
                s.isListed(),
                s.getPricePerCubicMeter(),
                s.getMaxVolumeCubicMeter(),
                s.getMaterialType(),
                s.getWarehouse().getWarehouseId()
        );
    }

}
