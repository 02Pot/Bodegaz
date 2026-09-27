package com.warehouse.system.DTO.Response;

import java.util.List;
import java.util.UUID;

public record ScrollResponse<T>(List<T> items, UUID nextCursor,boolean hasNext) {
}
