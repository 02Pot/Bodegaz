package com.warehouse.system.DTO.Lease.Response;

import com.warehouse.system.DTO.Lease.LeaseClause;
import com.warehouse.system.DTO.Lease.LeaseTemplateConfig;
import com.warehouse.system.Model.LeaseTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LeaseTemplateResponse(
        UUID id,
        UUID ownerId,                  // null = platform template
        String name,
        int version,
        boolean platformDefault,
        boolean active,
        LeaseTemplateConfig config,
        List<LeaseClause> clauses,
        Instant createdAt,
        Instant updatedAt
) {
    public static LeaseTemplateResponse from(LeaseTemplate t) {
        return new LeaseTemplateResponse(
                t.getId(), t.getOwnerId(), t.getName(), t.getVersion(),
                t.isPlatformDefault(), t.isActive(), t.getConfig(), t.getClauses(),
                t.getCreatedAt(), t.getUpdatedAt());
    }
}

