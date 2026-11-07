package com.warehouse.system.DTO.Lease;

import jakarta.validation.constraints.NotBlank;

public record LeaseClause(
        @NotBlank String key,
        @NotBlank String title,
        @NotBlank String body
) {
}
