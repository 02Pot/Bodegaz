package com.warehouse.system.DTO.Request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record WarehouseRequest(

        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Capacity is required")
        @Positive(message = "Capacity must be greater than 0")
        Double warehouseCapacityKg,

        Long viewCount,

        @NotBlank(message = "Address1 is required")
        String addressLine1,

        @NotBlank(message = "Address2 is required")
        String addressLine2,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "State Province is required")
        String stateProvince,

        @NotBlank(message = "Country is required")
        String country,

        @NotBlank(message = "Postal Code is required")
        String postalCode
) { }
