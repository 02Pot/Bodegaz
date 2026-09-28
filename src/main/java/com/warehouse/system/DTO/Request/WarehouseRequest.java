package com.warehouse.system.DTO.Request;


import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record WarehouseRequest(
        @NotBlank(message = "Id is required")
        UUID id,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Capacity is required")
        double warehouseCapacityKg,

        long viewCount,

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
