package com.warehouse.system.DTO.Response;

import com.warehouse.system.Enums.MaterialType;
import com.warehouse.system.Model.Storage;
import com.warehouse.system.Model.Warehouse;

import java.util.UUID;

public record StorageResponse(
        String blockName,
        String section,
        double maxWeight,
        boolean isAvailable,
        MaterialType materialType,
        UUID warehouseId,
        UUID sellerId
) {

    public static StorageResponse from(Storage s) {
        return new StorageResponse(
                s.getBlockName(),
                s.getSection(),
                s.getMaxWeight(),
                s.getAvailable(),
                s.getMaterialType(),
                s.getWarehouse().getWarehouseId(),
                s.getSellerId()
        );
    }

}
