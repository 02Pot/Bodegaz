package com.warehouse.system.DTO.Request;

import com.warehouse.system.Enums.InvoiceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvoiceRequest(
        @NotNull UUID leaseId,
        @NotNull LocalDateTime issueDate,
        @NotNull LocalDateTime dueDate,
        @NotNull @Positive BigDecimal totalAmount
) {}
