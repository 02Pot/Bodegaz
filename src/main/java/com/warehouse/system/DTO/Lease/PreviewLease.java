package com.warehouse.system.DTO.Lease;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PreviewLease(
        @NotNull UUID storageId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @DecimalMin("0.01") double rentedVolumeCubicMeter,
        @NotNull @DecimalMin("0.01") double rentedWeightKg
) {}