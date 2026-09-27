package com.warehouse.system.DTO.Request;


import lombok.Data;

import java.util.UUID;

@Data
public class WarehouseRequest {

    private UUID id;
    private String name;
    private double warehouseCapacityKg;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateProvince;
    private String country;
    private String postalCode;

}
