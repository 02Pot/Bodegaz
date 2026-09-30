package com.warehouse.system.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateHoldRequest(
        @NotNull UUID storageId,
        @NotBlank @Size(max = 32) String unitNumber,
        @NotNull LocalDateTime startDate,
        @NotNull LocalDateTime endDate
) {
}
