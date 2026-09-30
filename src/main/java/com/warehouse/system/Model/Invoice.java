package com.warehouse.system.Model;

import com.warehouse.system.Enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "invoice")
@RequiredArgsConstructor
public class Invoice {

    @Id
    @Column(name = "id",nullable = false,updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID invoiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leaseId",nullable = false)
    private LeaseAgreement leaseAgreement;

    @Column(name = "invoice_number",nullable = false)
    private String invoiceNumber;

    @Column(name = "issue_date",nullable = false)
    private LocalDateTime issueDate;

    @Column(name = "due_date",nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "total_amount",nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "invoice_status",nullable = false)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus invoiceStatus;

    @OneToOne(mappedBy = "invoice",cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;


}
