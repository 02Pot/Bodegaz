package com.warehouse.system.DTO.Response;

import java.util.UUID;

public record WarehouseImageResponse(
        UUID imageId,
        String url)
{

}
