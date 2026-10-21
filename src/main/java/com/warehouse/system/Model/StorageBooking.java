package com.warehouse.system.Model;

import com.warehouse.system.Enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "purchase_orders")
@RequiredArgsConstructor
public class StorageBooking {

    @Id
    @Column(name = "id",nullable = false,updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    @Column(name = "unit_number")
    private String unitNumber;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "rented_volume")
    private double rentedVolumeCubicMeter;

    @Column(name = "rented_weight")
    private double rentedWeightKg;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Version
    private Long version;

    @OneToOne(mappedBy = "storageBooking",cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private LeaseAgreement leaseAgreement;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private UserModel user;

    @ManyToOne
    @JoinColumn(name = "storage_id",nullable = false)
    private Storage storage;


}
