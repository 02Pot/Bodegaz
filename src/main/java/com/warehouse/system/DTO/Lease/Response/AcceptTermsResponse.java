package com.warehouse.system.DTO.Lease.Response;

import com.warehouse.system.Enums.LeaseStatus;
import com.warehouse.system.Model.LeaseAgreement;
import com.warehouse.system.Model.StorageBooking;

import java.time.LocalDateTime;
import java.util.UUID;

public record AcceptTermsResponse(
        UUID bookingId,
        UUID leaseId,
        String agreementNumber,
        LeaseStatus leaseStatus,
        String bookingStatus,
        LocalDateTime signDate
) {
    public static AcceptTermsResponse from(LeaseAgreement lease, StorageBooking booking) {
        return new AcceptTermsResponse(
                booking.getOrderId(),
                lease.getLeaseId(),
                lease.getAgreementNumber(),
                lease.getLeaseStatus(),
                booking.getStatus().name(),
                lease.getSignDate());
    }
}
