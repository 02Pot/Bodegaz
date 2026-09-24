package com.warehouse.system.DTO.Request;


import lombok.Data;

@Data
public class CreateWarehouseRequest {


    private String name;
    private double warehouseCapacityKg;
    private String addressBlock;
    private String city;
    private String barangay;
    private String country;
    private int zipcode;

}
