package com.warehouse.system.Model;

import com.warehouse.system.Enums.LeaseStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "lease")
@RequiredArgsConstructor
public class LeaseAgreement {

    @Id
    @Column(name = "id",nullable = false,updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID leaseId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingId",nullable = false)
    private StorageBooking storageBooking;

    @Column(name = "agreement_number",nullable = false)
    private String agreementNumber;

    @Column(name = "signed_date",nullable = false)
    private LocalDateTime signDate;

    //Implement terms condition entity
    @Column(name = "terms_condition")
    private String termsConditions;

    @Column(name = "lease_status")
    @Enumerated(EnumType.STRING)
    private LeaseStatus leaseStatus;

    @OneToMany(mappedBy = "leaseAgreement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Invoice> invoices = new ArrayList<>();

    @OneToMany(mappedBy = "leaseAgreementLine",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<InvoiceLineItem> invoiceLineItems = new ArrayList<>();


}
