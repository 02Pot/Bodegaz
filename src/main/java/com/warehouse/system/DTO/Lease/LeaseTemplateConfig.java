package com.warehouse.system.DTO.Lease;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record LeaseTemplateConfig(
        @Min(1) int termMonths,
        boolean autoRenew,
        @NotNull BillingCycle billingCycle,
        @Min(1) @Max(28) int billingDay,
        @Min(0) int gracePeriodDays,
        @NotNull @Valid LateFee lateFee,
        @NotNull @Valid Deposit deposit,
        @Min(0) int depositRefundDays,
        @Min(0) int noticeDaysToTerminate,
        @Min(1) int defaultDays,
        boolean insuranceRequired,
        @NotNull List<String> permittedItems,
        @NotBlank String accessHours
) {
    public enum BillingCycle { MONTHLY, QUARTERLY, ANNUAL }

    public record LateFee(
            @NotNull Type type,
            @NotNull @DecimalMin("0") BigDecimal value
    ) {
        public enum Type { PERCENT, FIXED }
    }

    public record Deposit(
            @NotNull Type type,
            @NotNull @DecimalMin("0") BigDecimal value
    ) {
        public enum Type { MONTHS_OF_RENT, FIXED }
    }
}