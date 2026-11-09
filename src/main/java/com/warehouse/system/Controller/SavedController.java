package com.warehouse.system.Controller;

import com.warehouse.system.DTO.Response.ScrollResponse;
import com.warehouse.system.DTO.Response.WarehouseResponse;
import com.warehouse.system.DTO.Warehouse.WarehouseFilter;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Service.Warehouse.SavedWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/warehouse/saved")
public class SavedController {

    private final SavedWarehouseService savedService;

    @PutMapping("/{warehouseId}/save")
    public ResponseEntity<Void> save(@PathVariable UUID warehouseId,
                                     @AuthenticationPrincipal UserModel user) {
        savedService.save(user.getId(), warehouseId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{warehouseId}/save")
    public ResponseEntity<Void> unsave(@PathVariable UUID warehouseId,
                                       @AuthenticationPrincipal UserModel user) {
        savedService.unsave(user.getId(), warehouseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    public ScrollResponse<WarehouseResponse> getSaved(
            @AuthenticationPrincipal UserModel user,
            @RequestParam(required = false) UUID cursor,
            WarehouseFilter filter,
            @RequestParam(defaultValue = "10") int size) {
        return savedService.getSaved(user.getId(), cursor, size,filter);
    }
}