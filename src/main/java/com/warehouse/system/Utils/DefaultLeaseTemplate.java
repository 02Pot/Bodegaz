package com.warehouse.system.Utils;

import com.warehouse.system.DTO.Lease.LeaseClause;
import com.warehouse.system.DTO.Lease.LeaseTemplateConfig;

import java.math.BigDecimal;
import java.util.List;

public final class DefaultLeaseTemplate {

    private DefaultLeaseTemplate() {}

    public static final String NAME = "Standard Monthly Storage Lease";

    public static LeaseTemplateConfig config() {
        return new LeaseTemplateConfig(
                1, true, LeaseTemplateConfig.BillingCycle.MONTHLY, 1, 5,
                new LeaseTemplateConfig.LateFee(LeaseTemplateConfig.LateFee.Type.PERCENT, new BigDecimal("5")),
                new LeaseTemplateConfig.Deposit(LeaseTemplateConfig.Deposit.Type.MONTHS_OF_RENT, BigDecimal.ONE),
                30, 30, 30, true,
                List.of("materials", "items", "goods"),
                "Mon-Sat 8:00 AM - 6:00 PM");
    }

    public static List<LeaseClause> clauses() {
        return List.of(
                new LeaseClause("premises", "Premises",
                        "The Lessor leases to the Tenant storage unit {{unit_number}} in block {{block_name}} {{section}} "
                                + "at {{warehouse_name}}, {{warehouse_address}}, with a rented volume of {{rented_volume}} cubic meters "
                                + "and a rented weight capacity of {{rented_weight}} kg."),
                new LeaseClause("term", "Term",
                        "This lease starts on {{start_date}} and ends on {{end_date}}. Either party may terminate with {{notice_days}} days' written notice."),
                new LeaseClause("payment", "Rent and Payment",
                        "Rent is {{rent_amount}} per {{billing_cycle}}, due on day {{billing_day}}. The total price for this booking is {{total_price}}. "
                                + "Payments more than {{grace_period_days}} days late incur a late fee of {{late_fee}}."),
                new LeaseClause("deposit", "Security Deposit",
                        "The Tenant pays a deposit of {{deposit_amount}} before move-in, refundable within {{deposit_refund_days}} days after move-out, less unpaid rent or damages."),
                new LeaseClause("use", "Permitted Use",
                        "Lawful storage only. Permitted: {{permitted_items}}."),
                new LeaseClause("access", "Access",
                        "Access is permitted during {{access_hours}}. Other times require written approval."),
                new LeaseClause("liability", "Insurance and Liability",
                        "{{insurance_notice}}The Lessor is liable for loss, theft, or damage to stored goods except where caused by the Tenant's negligence."),
                new LeaseClause("default", "Default",
                        "If rent is unpaid for {{default_days}} days, the Lessor may deny access and, after written notice, act as permitted by law."),
                new LeaseClause("entire", "Entire Agreement",
                        "This agreement and any signed amendments form the full agreement between the parties.")
        );
    }
}