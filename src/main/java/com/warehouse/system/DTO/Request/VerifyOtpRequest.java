package com.warehouse.system.DTO.Request;

import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(
        @NotBlank(message = "OTP is required")
        String otp
) { }
