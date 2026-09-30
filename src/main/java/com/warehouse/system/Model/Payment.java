package com.warehouse.system.Model;

import com.warehouse.system.Enums.PaymentMethod;
import com.warehouse.system.Enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name = "payment")
@RequiredArgsConstructor
public class Payment {

    @Id
    @Column(name = "id",nullable = false,updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoiceId",nullable = false)
    private Invoice invoice;

    @Column(name = "transaction_number")
    private String transactionNumber;

    @CreationTimestamp
    @Column(name = "payment_date", updatable = false)
    private Instant paymentDate;

    @Column(name = "total_amount",nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "payment_method",nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_status",nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

}
