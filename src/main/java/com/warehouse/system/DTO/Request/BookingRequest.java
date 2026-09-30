package com.warehouse.system.DTO.Request;

import com.warehouse.system.Enums.UserType;

import java.util.UUID;

public record BookingRequest(
        UUID buyerId,
        String buyerEmail,
        UserType buyerRole
) {
}
