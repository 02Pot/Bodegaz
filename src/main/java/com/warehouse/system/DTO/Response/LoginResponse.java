package com.warehouse.system.DTO.Response;

import com.warehouse.system.DTO.TokenPair;
import com.warehouse.system.Enums.UserType;
import com.warehouse.system.Model.UserModel;

import java.util.UUID;

public record LoginResponse(
        TokenPair tokenPair,
        UUID id,
        String email,
        String message,
        UserType userType
) {
    public static LoginResponse from(UserModel u,TokenPair tokenPair,String message){
        return new LoginResponse(
                tokenPair,
                u.getId(),
                u.getEmail(),
                message,
                u.getUserType()
        );
    }


}
