package com.warehouse.system.DTO.Lease;

import com.warehouse.system.Enums.LeaseStatus;
import com.warehouse.system.Model.LeaseAgreement;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record LeaseView(
        UUID leaseId,
        UUID bookingId,
        String agreementNumber,
        String termsConditions,             // fully rendered agreement text
        String termsHash,                   // client sends this back when accepting
        Map<String, String> keyTerms,       // placeholders, for a highlighted summary box
        LeaseStatus status,
        LocalDateTime signDate              // null until accepted
) {
    public static LeaseView from(LeaseAgreement lease) {
        return new LeaseView(
                lease.getLeaseId(),
                lease.getStorageBooking().getOrderId(),
                lease.getAgreementNumber(),
                lease.getTermsConditions(),
                lease.getTermsHash(),
                lease.getTermsSnapshot().placeholders(),
                lease.getLeaseStatus(),
                lease.getSignDate());
    }
}
