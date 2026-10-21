package com.warehouse.system.DTO.Interface;

import java.util.UUID;

public interface BookedStats {
    UUID getWarehouseId();
    long getBookedCount();
}
