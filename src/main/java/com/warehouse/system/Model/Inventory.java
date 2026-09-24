package com.warehouse.system.Model;

import com.warehouse.system.Enums.MaterialType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "Inventory")
@RequiredArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue
    @Column(name = "inventory_id",updatable = false,nullable = false)
    private UUID inventoryId;

    private String productTitle;

    @Column(name = "length_meters",nullable = false)
    private double lengthMeters;

    @Column(name = "breadth_meters",nullable = false)
    private double breadthMeters;

    @Column(name = "height_meters",nullable = false)
    private double heightMeters;

    @Column(name = "weight_kg",nullable = false)
    private double weightKg;

    @Column(name = "quantity",nullable = false)
    private double quantity;

    @Column(name = "material_type",nullable = false)
    private MaterialType materialType;

    @Column(name = "restocked_at")
    private LocalDateTime restockedAt;

    @ManyToOne
    @JoinColumn(name = "id",nullable = false)
    private UserModel user;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private StorageBooking booking;

}
