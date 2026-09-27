package com.warehouse.system.DTO.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Email
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank
        @Size(min = 8, message = "Password must be at lesat 8 characters")
        String password
) { }
