package com.warehouse.system.Controller;

import com.warehouse.system.DTO.Request.StorageRequest;
import com.warehouse.system.DTO.Request.StorageUpdateRequest;
import com.warehouse.system.DTO.Response.StorageResponse;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Service.Warehouse.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/warehouses/{warehouseId}/storages")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<List<StorageResponse>> create(
            @PathVariable UUID warehouseId,
            @Valid @RequestBody StorageRequest req,
            @AuthenticationPrincipal UserModel user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storageService.addStorage(req,user.getId(), warehouseId));
    }

    @GetMapping
    public List<StorageResponse> list(@PathVariable UUID warehouseId) {
        return storageService.getStorages(warehouseId);
    }

    @DeleteMapping("/{storageId}")
    public ResponseEntity<Void> delete(@PathVariable UUID warehouseId,
                                       @PathVariable UUID storageId,
                                       @AuthenticationPrincipal UserModel user) {
        storageService.deleteStorage(warehouseId, storageId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{storageId}")
    public ResponseEntity<StorageResponse> update(
            @PathVariable UUID storageId,
            @RequestBody @Valid StorageUpdateRequest request,
            @AuthenticationPrincipal UserModel user
    ){
        return ResponseEntity.ok(storageService.updateStorage(
                storageId,request,
                user.getId()));
    }

}
