package com.warehouse.system.Service.Invoice;

import com.warehouse.system.DTO.Request.InvoiceRequest;
import com.warehouse.system.DTO.Response.InvoiceResponse;
import com.warehouse.system.DTO.Response.ScrollResponse;
import com.warehouse.system.Enums.InvoiceStatus;
import com.warehouse.system.Model.Invoice;
import com.warehouse.system.Repository.InvoiceRepository;
import com.warehouse.system.Repository.LeaseAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final LeaseAgreementRepository leaseAgreementRepository;

    public ScrollResponse<Invoice> getAllInvoiceCursor(UUID cursor, int size){
        UUID lastId = cursor != null ? cursor : new UUID(0L,0L);
        Pageable pageable = PageRequest.of(0, size, Sort.by("invoiceId").ascending());
        Slice<Invoice> slice = invoiceRepository.findByInvoiceIdGreaterThanOrderByInvoiceIdAsc(lastId,pageable);

        UUID nextCursor = slice.hasContent()
                ? slice.getContent().getLast().getInvoiceId()
                : null;

        return new ScrollResponse<>(slice.getContent(),nextCursor, slice.hasContent());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getById(UUID id) {
        return InvoiceResponse.from(find(id));
    }

    @Transactional
    public InvoiceResponse create(InvoiceRequest req) {
        leaseAgreementRepository.findById(req.leaseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Lease not found"));

        validateExist(req);
        validateDates(req);

        Invoice invoice = new Invoice();
        invoice.setInvoiceStatus(InvoiceStatus.DRAFT);

        apply(invoice, req);

        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAll(InvoiceStatus status, Pageable pageable) {
        Page<Invoice> page = (status == null)
                ? invoiceRepository.findAll(pageable)
                : invoiceRepository.findByInvoiceStatus(status, pageable);
        return page.map(InvoiceResponse::from);
    }

    @Transactional
    public InvoiceResponse update(UUID id, InvoiceRequest req) {
        Invoice invoice = find(id);
        if(invoice.getInvoiceStatus() != InvoiceStatus.DRAFT){
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
        }
        validateDates(req);
        invoice.setInvoiceStatus(InvoiceStatus.IN_REVIEW);
        apply(invoice, req);
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public void delete(UUID id) {
        invoiceRepository.delete(find(id));
    }

    private Invoice find(UUID id) {
        return invoiceRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found: " + id));
    }

    private void validateExist(InvoiceRequest req){
        if(invoiceRepository.existsByLeaseAgreement_LeaseId(req.leaseId())){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Invoice already exist");
        }
    }

    private void validateDates(InvoiceRequest req) {
        if (req.dueDate().isBefore(req.issueDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date cannot be before issue date");
        }
    }

    //SUBMITTED INVOICE
    //APPROVED INVOICE
    //DECLINED INVOICE
    //PAID INVOICE
    //HOLD INVOICE
    //OVERDUE INVOICE

    private void apply(Invoice invoice, InvoiceRequest req) {
        invoice.setLeaseAgreement(leaseAgreementRepository.findById(req.leaseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease not found: " + req.leaseId())));
        invoice.setInvoiceNumber("INV-" + UUID.randomUUID());
        invoice.setIssueDate(req.issueDate());
        invoice.setDueDate(req.dueDate());
        invoice.setTotalAmount(req.totalAmount());
    }

}
