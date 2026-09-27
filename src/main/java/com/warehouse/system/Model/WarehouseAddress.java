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

    @Column(name = "address_line1",nullable = false)
    private String addressLine1;

    @Column(name = "address_line2",nullable = false)
    private String addressLine2;

    @Column(name = "city",nullable = false)
    private String city;

    @Column(name = "state_province",nullable = false)
    private String stateProvince;

    @Column(name = "country",nullable = false)
    private String country;

    @Column(name = "postalCode",nullable = false)
    private String postalCode;

    @OneToOne(mappedBy = "warehouseAddress",cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Warehouse warehouse;

}
