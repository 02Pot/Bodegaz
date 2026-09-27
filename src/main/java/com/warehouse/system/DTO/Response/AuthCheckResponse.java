package com.warehouse.system.DTO.Response;

import com.warehouse.system.Enums.UserType;
import com.warehouse.system.Model.UserModel;

import java.util.UUID;

public record AuthCheckResponse(
        UUID id,
        String email,
        String name,
        UserType userType,
        boolean isVerified,
        boolean isRegistered
) {
    public static AuthCheckResponse from(UserModel u){
        return new AuthCheckResponse(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.getUserType(),
                u.isVerified(),
                u.isRegistered()
        );
    }

}
