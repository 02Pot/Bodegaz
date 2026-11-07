package com.warehouse.system.DTO.Lease;

import java.util.Map;

public record LeasePreview(
        String termsConditions,
        Map<String, String> keyTerms,
        String termsHash
) {}