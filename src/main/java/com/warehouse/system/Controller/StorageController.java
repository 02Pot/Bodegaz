package com.warehouse.system.Controller;

import com.warehouse.system.DTO.Request.StorageRequest;
import com.warehouse.system.DTO.Response.StorageResponse;
import com.warehouse.system.Service.Warehouse.StorageService;
import com.warehouse.system.Utils.GetCurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/warehouses/{warehouseId}/storages")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;
    private final GetCurrentUserId userIdService;

    @PostMapping
    public ResponseEntity<List<StorageResponse>> create(
            @PathVariable UUID warehouseId,
            @Valid @RequestBody StorageRequest req,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storageService.addStorage(req, userIdService.currentUserId(authentication), warehouseId));
    }

    @GetMapping
    public List<StorageResponse> list(@PathVariable UUID warehouseId) {
        return storageService.getStorages(warehouseId);
    }

    @DeleteMapping("/{storageId}")
    public ResponseEntity<Void> delete(@PathVariable UUID warehouseId,
                                       @PathVariable UUID storageId,
                                       Authentication authentication) {
        storageService.deleteStorage(warehouseId, storageId, userIdService.currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

}
