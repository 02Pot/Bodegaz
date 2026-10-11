package com.warehouse.system.Model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Setter
@Getter
@Table(name = "storage_type", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@NoArgsConstructor
public class StorageType {

    @Id
    @GeneratedValue
    @Column(name = "storage_type_id",updatable = false,nullable = false)
    private UUID storageTypeId;

    @Column(nullable = false)
    private String name;

    @Column(name = "length_meters",nullable = false)
    private double lengthMeters;

    @Column(name = "breadth_meters",nullable = false)
    private double breadthMeters;

    @Column(name = "height_meters",nullable = false)
    private double heightMeters;

    @Column(name = "capacity_weight",nullable = false)
    private double capacityWeight;

}
