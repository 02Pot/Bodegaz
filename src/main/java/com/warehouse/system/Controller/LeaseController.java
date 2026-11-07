package com.warehouse.system.Controller;


import com.warehouse.system.DTO.Lease.LeasePreview;
import com.warehouse.system.DTO.Lease.LeaseView;
import com.warehouse.system.DTO.Lease.PreviewLease;
import com.warehouse.system.DTO.Lease.Request.AcceptTermsRequest;
import com.warehouse.system.DTO.Lease.Response.AcceptTermsResponse;
import com.warehouse.system.DTO.Lease.Response.LeaseSummaryResponse;
import com.warehouse.system.Model.LeaseAgreement;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Service.Lease.LeaseSnapshotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LeaseController {

    private final LeaseSnapshotService leaseService;

    @GetMapping("/bookings/{bookingId}/lease")
    public LeaseView getLease(@PathVariable UUID bookingId, @AuthenticationPrincipal UserModel user) {
        return leaseService.getLeaseView(bookingId,user.getId());
    }

    @PostMapping("/leases/preview")
    public LeasePreview preview(@Valid @RequestBody PreviewLease body,
                                @AuthenticationPrincipal UserModel user) {
        return leaseService.preview(body, user);
    }

    @PostMapping("/bookings/{bookingId}/accept-terms")
    public AcceptTermsResponse acceptTerms(@PathVariable UUID bookingId,
                                           @Valid @RequestBody AcceptTermsRequest body,
                                           @AuthenticationPrincipal UserModel user,
                                           HttpServletRequest request) {
        LeaseAgreement lease = leaseService.acceptTerms(
                bookingId, user.getId(), request.getRemoteAddr(), body.termsHash());
        return AcceptTermsResponse.from(lease, lease.getStorageBooking());
    }

    @GetMapping("/leases/mine")
    public List<LeaseSummaryResponse> myLeases(@AuthenticationPrincipal UserModel user) {
        return leaseService.listRenterLeases(user.getId());
    }

    @GetMapping("/leases/owner")
    public List<LeaseSummaryResponse> ownerLeases(@AuthenticationPrincipal UserModel user) {
        return leaseService.listOwnerLeases(user.getId());
    }
}
