package com.warehouse.system.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "warehouse")
@RequiredArgsConstructor
public class Warehouse {

    @Id
    @Column(name = "warehouse_id",updatable = false,nullable = false)
    @GeneratedValue
    private UUID warehouseId;

    @Column(name = "warehouse_name",nullable = false)
    private String name;

    @Column(name = "warehouse_capacity",nullable = false)
    private double warehouseCapacityKg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_address_id",nullable = false,unique = true)
    private WarehouseAddress warehouseAddress;

    @OneToMany(mappedBy = "warehouse",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<Storage> storage = new ArrayList<>();

}
