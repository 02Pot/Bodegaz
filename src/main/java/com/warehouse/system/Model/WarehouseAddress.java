package com.warehouse.system.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Entity
@Data
@Table(name = "warehouse_address")
@RequiredArgsConstructor
public class WarehouseAddress {

    @Id
    @GeneratedValue
    @Column(name = "address_id",updatable = false,nullable = false)
    private UUID addressId;

    @Column(name = "address_block",nullable = false)
    private String addressBlock;

    @Column(name = "city",nullable = false)
    private String city;

    @Column(name = "barangay",nullable = false)
    private String barangay;

    @Column(name = "country",nullable = false)
    private String country;

    @Column(name = "zipcode",nullable = false)
    private int zipcode;

    @OneToOne(mappedBy = "warehouseAddress",cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Warehouse warehouse;

}
