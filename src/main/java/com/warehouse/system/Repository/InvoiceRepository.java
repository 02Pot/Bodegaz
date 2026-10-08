package com.warehouse.system.Repository;

import com.warehouse.system.Enums.InvoiceStatus;
import com.warehouse.system.Model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByInvoiceNumber(String number);
    Slice<Invoice> findByInvoiceIdGreaterThanOrderByInvoiceIdAsc(UUID id, Pageable pageable);
    boolean existsByInvoiceNumber(String invoiceNumber);
    boolean existsByLeaseAgreementLeaseId(UUID id);
    boolean existsByInvoiceNumberAndInvoiceIdNot(String invoiceNumber, UUID invoiceId);
    boolean existsByLeaseAgreementLeaseIdAndInvoiceIdNot(UUID leaseId, UUID invoiceId);
    Page<Invoice> findByInvoiceStatus(InvoiceStatus status, Pageable pageable);
}
