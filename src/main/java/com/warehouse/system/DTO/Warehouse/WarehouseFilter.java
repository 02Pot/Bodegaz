package com.warehouse.system.DTO.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WarehouseFilter(
        String search, String city,
        Double minCapacity,
        Double minArea, Double maxArea,
        BigDecimal minBudget, BigDecimal maxBudget,
        String sortOrder,
        LocalDate customDate
) {
}
