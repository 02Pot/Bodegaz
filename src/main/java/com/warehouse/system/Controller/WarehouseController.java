package com.warehouse.system.Controller;

import com.warehouse.system.DTO.Request.WarehouseRequest;
import com.warehouse.system.DTO.Response.ScrollResponse;
import com.warehouse.system.DTO.Response.WarehouseResponse;
import com.warehouse.system.Model.Warehouse;
import com.warehouse.system.Service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController("/api/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/all")
    public ResponseEntity<ScrollResponse<Warehouse>> getAll(
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
            @PathVariable  UUID id
    ){
        return ResponseEntity.ok(warehouseService.addWarehouse(request,id));
    }

    @PutMapping
    public ResponseEntity<WarehouseResponse> updateWarehouse(
            @Valid @RequestBody WarehouseRequest request,
            @PathVariable UUID id
    ){
        return ResponseEntity.ok(warehouseService.updateWarehouse(request,id));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteWarehouse(
            @PathVariable UUID id
    ){
        return ResponseEntity.ok(warehouseService.delete(id));
    }

    @GetMapping("/trending")
    public ResponseEntity<Page<WarehouseResponse>> getTrendingWarehouse(
            @PathVariable UUID id,
            @RequestParam int size
    ){
        return ResponseEntity.ok(warehouseService.getByTrending(id,size));
    }

    @GetMapping("/newest")
    public ResponseEntity<Page<WarehouseResponse>> getNewestWarehouse(
            @PathVariable UUID id,
            @RequestParam int size
    ){
        return ResponseEntity.ok(warehouseService.getByNewest(id,size));
    }

}
