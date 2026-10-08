package com.warehouse.system.Utils;

import com.warehouse.system.Model.UserModel;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GetCurrentUserId {

    public UUID currentUserId(Authentication auth) {
        UserModel user = (UserModel) auth.getPrincipal();
        return user.getId();
    }

}
