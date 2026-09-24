package com.warehouse.system.DTO.Response;

import com.warehouse.system.DTO.TokenPair;
import com.warehouse.system.Enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private TokenPair token;
    private UUID id;
    private String email;
    private String name;
    private String message;
    private UserType userType;
}
