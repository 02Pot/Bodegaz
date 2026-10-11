package com.warehouse.system.DTO.Response;

import com.warehouse.system.Model.StorageType;

import java.util.UUID;

public record StorageTypeResponse(
        UUID storageTypeId,
        String name,
        double lengthMeters,
        double breadthMeters,
        double heightMeters,
        double capacityWeight,
        double volumeCubicMeters

) {
    public static StorageTypeResponse from(StorageType t) {
        return new StorageTypeResponse(
                t.getStorageTypeId(),
                t.getName(),
                t.getLengthMeters(),
                t.getBreadthMeters(),
                t.getHeightMeters(),
                t.getCapacityWeight(),
                t.getLengthMeters() * t.getBreadthMeters() * t.getHeightMeters()
        );
    }
}