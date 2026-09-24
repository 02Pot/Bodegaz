package com.warehouse.system.DTO.Request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "OTP is required")
    private String otp;
}