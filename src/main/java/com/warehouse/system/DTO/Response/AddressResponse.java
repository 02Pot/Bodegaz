package com.warehouse.system.DTO.Response;

import com.warehouse.system.Model.WarehouseAddress;

public record AddressResponse(
        String addressLine1,
        String addressLine2,
        String city,
        String stateProvince,
        String country,
        String postalCode
) {
    public static AddressResponse from(WarehouseAddress a) {
        return new AddressResponse(
                a.getAddressLine1(), a.getAddressLine2(), a.getCity(),
                a.getStateProvince(), a.getCountry(), a.getPostalCode());
    }
}
