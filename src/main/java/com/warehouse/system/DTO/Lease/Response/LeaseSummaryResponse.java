package com.warehouse.system.DTO.Lease.Response;

import com.warehouse.system.Enums.LeaseStatus;
import com.warehouse.system.Model.LeaseAgreement;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeaseSummaryResponse(
        UUID leaseId,
        UUID bookingId,
        String agreementNumber,
        String unitNumber,
        String tenantName,
        LeaseStatus status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime signDate
) {
    public static LeaseSummaryResponse from(LeaseAgreement lease) {
        var booking = lease.getStorageBooking();
        var terms = lease.getTermsSnapshot().placeholders();
        return new LeaseSummaryResponse(
                lease.getLeaseId(),
                booking.getOrderId(),
                lease.getAgreementNumber(),
                terms.get("unit_number"),
                terms.get("tenant_name"),
                lease.getLeaseStatus(),
                booking.getStartDate(),
                booking.getEndDate(),
                lease.getSignDate());
    }
}