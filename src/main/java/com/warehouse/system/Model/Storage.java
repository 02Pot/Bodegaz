package com.warehouse.system.Model;

import com.warehouse.system.Enums.MaterialType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "storage")
@RequiredArgsConstructor
public class Storage {

    @Id
    @GeneratedValue
    @Column(name = "storage_id",updatable = false,nullable = false)
    private UUID storageId;

    @Column(name = "block_name", nullable = false)
    private String blockName;

    @Column(name = "section", nullable = false)
    private String section;

    @Column(name = "max_add_weight")
    private double maxAddWeight;

    @Column(name = "material_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MaterialType materialType;

    @Column(name = "available_area",nullable = false)
    private String availableArea;

    @Column(name = "seller_id",nullable = false)
    private UUID sellerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id",nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_type_id",nullable = false)
    private StorageType storageType;

    @OneToMany(mappedBy = "storage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StorageBooking> storageBookings = new ArrayList<>();

}
