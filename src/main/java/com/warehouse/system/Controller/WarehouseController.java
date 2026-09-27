package com.warehouse.system.Controller;

import com.warehouse.system.DTO.Request.WarehouseRequest;
import com.warehouse.system.DTO.Response.ScrollResponse;
import com.warehouse.system.DTO.Response.WarehouseResponse;
import com.warehouse.system.Model.Warehouse;
import com.warehouse.system.Service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public ResponseEntity<ScrollResponse<Warehouse>> getAll(
            @RequestParam(required = false)UUID cursor,
            @RequestParam(defaultValue = "10") int size
            ){
        return ResponseEntity.ok(warehouseService.getAllWarehouse(cursor,size));
    }

    @PostMapping
    public ResponseEntity<WarehouseResponse> addWarehouse(
            @RequestBody WarehouseRequest request,
            @PathVariable  UUID id){
        return ResponseEntity.ok(warehouseService.addWarehouse(request,id));
    }


}
