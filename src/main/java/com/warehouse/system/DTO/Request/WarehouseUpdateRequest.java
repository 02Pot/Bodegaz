package com.warehouse.system.DTO.Request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WarehouseUpdateRequest(
        String name,

        @Positive
        Double warehouseCapacityKg,

        String addressLine1,
        String addressLine2,
        String city,
        String stateProvince,
        String country,
        String postalCode
) {
}
