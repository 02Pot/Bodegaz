package com.warehouse.system.DTO.Lease.Request;


import com.warehouse.system.DTO.Lease.LeaseClause;
import com.warehouse.system.DTO.Lease.LeaseTemplateConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateLeaseTemplateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @Valid LeaseTemplateConfig config,
        @NotEmpty @Valid List<LeaseClause> clauses
) {}
