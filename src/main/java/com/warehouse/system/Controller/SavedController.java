package com.warehouse.system.Controller;

import com.warehouse.system.DTO.Response.ScrollResponse;
import com.warehouse.system.DTO.Response.WarehouseResponse;
import com.warehouse.system.Service.Warehouse.SavedWarehouseService;
import com.warehouse.system.Utils.GetCurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/warehouse/saved")
public class SavedController {

    private final SavedWarehouseService savedService;
    private final GetCurrentUserId getCurrentUserId;

    @PutMapping("/{warehouseId}/save")
    public ResponseEntity<Void> save(@PathVariable UUID warehouseId,
                                     Authentication auth) {
        savedService.save(getCurrentUserId.currentUserId(auth), warehouseId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{warehouseId}/save")
    public ResponseEntity<Void> unsave(@PathVariable UUID warehouseId,
                                       Authentication auth) {
        savedService.unsave(getCurrentUserId.currentUserId(auth), warehouseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    public ScrollResponse<WarehouseResponse> getSaved(Authentication auth,
                                                      @RequestParam(required = false) UUID cursor,
                                                      @RequestParam(defaultValue = "10") int size) {
        return savedService.getSaved(getCurrentUserId.currentUserId(auth), cursor, size);
    }
}