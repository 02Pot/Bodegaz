package com.warehouse.system.DTO.Response;

import com.warehouse.system.Enums.UserType;
import com.warehouse.system.Model.UserModel;

import java.util.UUID;

public record LoginResponse(
        UUID id,
        String email,
        String message,
        UserType userType
) {
    public static LoginResponse from(UserModel u,String message){
        return new LoginResponse(
                u.getId(),
                u.getEmail(),
                message,
                u.getUserType()
        );
    }

}
