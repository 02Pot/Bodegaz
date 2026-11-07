package com.warehouse.system.DTO.Lease.Request;

import com.warehouse.system.DTO.Lease.LeaseClause;
import com.warehouse.system.DTO.Lease.LeaseTemplateConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateLeaseTemplateRequest(
        @Size(min = 1, max = 120) String name,
        @Valid LeaseTemplateConfig config,
        @Valid List<LeaseClause> clauses
) {}