package com.warehouse.system.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "storage_type")
@RequiredArgsConstructor
public class StorageType {

    @Id
    @GeneratedValue
    @Column(name = "storage_type_id",updatable = false,nullable = false)
    private UUID storageTypeId;

    @Column(name = "length_meters",nullable = false)
    private double lengthMeters;

    @Column(name = "breadth_meters",nullable = false)
    private double breadthMeters;

    @Column(name = "height_meters",nullable = false)
    private double heightMeters;

    @Column(name = "capacity_weight",nullable = false)
    private double capacityWeight;

    @Column(name = "units_available",nullable = false)
    private double unitsAvailable;

    @OneToMany(mappedBy = "storageType",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<Storage> storages = new ArrayList<>();

}
