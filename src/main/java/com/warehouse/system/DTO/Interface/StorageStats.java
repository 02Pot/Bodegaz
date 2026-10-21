package com.warehouse.system.DTO.Interface;

import java.util.UUID;

public interface StorageStats {
    UUID getWarehouseId();
    long getStorageCount();
    double getAllocatedKg();
}

