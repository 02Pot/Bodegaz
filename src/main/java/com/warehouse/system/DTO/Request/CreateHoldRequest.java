package com.warehouse.system.DTO.Request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateHoldRequest(
        @NotNull UUID storageId,
        @Positive @NotNull double rentedVolumeCubicMeter,
        @Positive @NotNull double rentedWeightKg,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
