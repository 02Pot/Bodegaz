package com.warehouse.system.DTO.Response;

import com.warehouse.system.Enums.AuthAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private boolean success;
    private String message;
    private AuthAction action;
}
