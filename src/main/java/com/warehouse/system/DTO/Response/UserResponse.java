package com.warehouse.system.DTO.Response;

import com.warehouse.system.Enums.AuthAction;
import com.warehouse.system.Model.UserModel;

public record UserResponse(
         boolean success,
         String message,
         AuthAction action
) {
    public static UserResponse success(String message, AuthAction action) {
        return new UserResponse(true, message, action);
    }

    public static UserResponse failure(String message) {
        return new UserResponse(false, message, null);
    }

}
