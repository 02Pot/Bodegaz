package com.warehouse.system.DTO.Response;

import com.warehouse.system.Enums.UserType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthCheckResponse {
    private UUID id;
    private String email;
    private String name;
    private String userType;
    private boolean isVerified;
    private boolean isRegistered;
}
