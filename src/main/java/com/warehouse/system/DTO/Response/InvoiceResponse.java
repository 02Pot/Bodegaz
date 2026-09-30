package com.warehouse.system.DTO.Response;

import com.warehouse.system.Enums.InvoiceStatus;
import com.warehouse.system.Model.Invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvoiceResponse(
        UUID storageId,
        String invoiceNumber,
        LocalDateTime issueDate,
        LocalDateTime dueDate,
        BigDecimal totalAmount,
        InvoiceStatus invoiceStatus
) {
    public static InvoiceResponse from(Invoice i){
        return new InvoiceResponse(
                i.getInvoiceId(),
                i.getInvoiceNumber(),
                i.getIssueDate(),
                i.getDueDate(),
                i.getTotalAmount(),
                i.getInvoiceStatus()
        );
    }
}
