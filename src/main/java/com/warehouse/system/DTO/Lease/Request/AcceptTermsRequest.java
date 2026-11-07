package com.warehouse.system.DTO.Lease.Request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /bookings/{bookingId}/accept-terms.
 * The IP address is NOT here: the server reads it from the request, never from the client.
 */
public record AcceptTermsRequest(

        @AssertTrue(message = "You must agree to the terms to continue")
        boolean agreed,                       // the "I have read and agree" checkbox

        @NotBlank @Size(min = 64, max = 64)
        String termsHash
) {}
