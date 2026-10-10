package com.warehouse.system.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Setter
@Getter
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

    private Long viewCount;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "warehouse_address_id",nullable = false,unique = true)
    private WarehouseAddress warehouseAddress;

    @OneToMany(mappedBy = "warehouse",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<Storage> storage = new ArrayList<>();

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WarehouseImage> images = new ArrayList<>();

}
