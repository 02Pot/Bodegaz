package com.warehouse.system.Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
@RequiredArgsConstructor
public class InvoiceLineItem {

    @Id
    @Column(name = "id",nullable = false,updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID lineItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id",nullable = false)
    private LeaseAgreement leaseAgreementLine;

    @Column(name = "description",nullable = false)
    private String description;

    @Column(name = "quantity",nullable = false)
    private int quantity;

    @Column(name = "unit_price",nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "lineTotal",nullable = false)
    private BigDecimal lineTotal;

}
