package com.warehouse.system.Model;

import com.warehouse.system.DTO.Lease.LeaseTermsSnapshot;
import com.warehouse.system.Enums.LeaseStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
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
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID leaseId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private StorageBooking storageBooking;

    @Column(name = "agreement_number", nullable = false, unique = true)
    private String agreementNumber;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "template_version")
    private Integer templateVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "terms_snapshot", nullable = false, columnDefinition = "jsonb")
    private LeaseTermsSnapshot termsSnapshot;

    @Column(name = "terms_condition", columnDefinition = "text")
    private String termsConditions;

    @Column(name = "terms_hash", length = 64)
    private String termsHash;

    @Column(name = "signed_date")
    private LocalDateTime signDate;

    @Column(name = "accepted_by")
    private UUID acceptedBy;

    @Column(name = "accepted_ip", length = 45)
    private String acceptedIp;

    @Column(name = "lease_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private LeaseStatus leaseStatus = LeaseStatus.REVIEWING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "leaseAgreement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Invoice> invoices = new ArrayList<>();

    @OneToMany(mappedBy = "leaseAgreementLine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLineItem> invoiceLineItems = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LeaseAgreement other)) return false;
        return leaseId != null && leaseId.equals(other.leaseId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
