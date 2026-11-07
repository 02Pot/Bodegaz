package com.warehouse.system.DTO.Lease;

import java.util.List;
import java.util.Map;

public record LeaseTermsSnapshot(
        LeaseTemplateConfig config,
        List<LeaseClause> clauses,
        Map<String, String> placeholders
) {}
