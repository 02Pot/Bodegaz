package com.warehouse.system.Controller;

import com.warehouse.system.DTO.Request.WarehouseRequest;
import com.warehouse.system.DTO.Response.BookingResponse;
import com.warehouse.system.DTO.Response.ScrollResponse;
import com.warehouse.system.DTO.Response.WarehouseResponse;
import com.warehouse.system.Model.StorageBooking;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Model.Warehouse;
import com.warehouse.system.Service.Warehouse.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final Clock clock;

    @GetMapping("/all")
    public ResponseEntity<ScrollResponse<WarehouseResponse>> getAll(
            @RequestParam(required = false)UUID cursor,
            @RequestParam(defaultValue = "10") int size
    ){
        return ResponseEntity.ok(warehouseService.getAllWarehouse(cursor,size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponse> getById(
            @PathVariable UUID id
    ){
        return ResponseEntity.ok(warehouseService.getById(id));
    }

    @PostMapping("/add")
    public ResponseEntity<WarehouseResponse> addWarehouse(
            @Valid @RequestBody WarehouseRequest request,
            Authentication auth
    ){
        return ResponseEntity.ok(warehouseService.addWarehouse(request,currentUserId(auth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WarehouseResponse> updateWarehouse(
            @Valid @RequestBody WarehouseRequest request,
            Authentication auth
    ){
        return ResponseEntity.ok(warehouseService.updateWarehouse(request,currentUserId(auth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWarehouse(
            Authentication auth,
            @PathVariable UUID id
    ){
        return ResponseEntity.ok(warehouseService.delete(currentUserId(auth),id));
    }

    @GetMapping("/trending")
    public ResponseEntity<Page<WarehouseResponse>> getTrendingWarehouse(
            Authentication auth,
            @RequestParam int size
    ){
        return ResponseEntity.ok(warehouseService.getByTrending(currentUserId(auth),size));
    }

    @GetMapping("/newest")
    public ResponseEntity<Page<WarehouseResponse>> getNewestWarehouse(
            Authentication auth,
            @RequestParam int size
    ){
        return ResponseEntity.ok(warehouseService.getByNewest(currentUserId(auth),size));
    }

    private UUID currentUserId(Authentication auth) {
        UserModel user = (UserModel) auth.getPrincipal();
        return user.getId();
    }

}
