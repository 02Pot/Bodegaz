package com.warehouse.system.Service;

import com.warehouse.system.DTO.Request.WarehouseRequest;
import com.warehouse.system.DTO.Response.ScrollResponse;
import com.warehouse.system.DTO.Response.WarehouseResponse;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Model.Warehouse;
import com.warehouse.system.Model.WarehouseAddress;
import com.warehouse.system.Repository.UserModelRepository;
import com.warehouse.system.Repository.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final UserModelRepository userModelRepository;

    public ScrollResponse<Warehouse> getAllWarehouse(UUID cursor, int size) {
        UUID lastId = cursor != null ? cursor : new UUID(0L,0L);
        Pageable pageable = PageRequest.of(0,size);
        Slice<Warehouse> slice = warehouseRepository.findByWarehouseIdGreaterThanOrderByWarehouseIdAsc(lastId,pageable);

        UUID nextCursor = slice.hasContent()
                ? slice.getContent().getLast().getWarehouseId() : null;

        return new ScrollResponse<>(slice.getContent(),nextCursor,slice.hasNext());
    }

    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public WarehouseResponse getById(UUID id) {
        return WarehouseResponse.from(warehouseRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Warehouse not found: " + id)));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public Page<WarehouseResponse> getByTrending(UUID id, int size) {
        UserModel user = userModelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(0, size);
        Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);

        Page<Warehouse> trending = warehouseRepository.findTrending(oneDayAgo, pageable);
        return trending.map(warehouse -> new WarehouseResponse(
                warehouse.getWarehouseId(),
                warehouse.getName(),
                warehouse.getWarehouseCapacityKg(),
                user.getId(),
                warehouse.getWarehouseAddress().getAddressId()
        ));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public Page<WarehouseResponse> getByNewest(UUID id,int size){
        UserModel user = userModelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(0,size);
        Page<Warehouse> newest = warehouseRepository.findNewest(pageable);

        return newest.map(warehouse -> new WarehouseResponse(
                warehouse.getWarehouseId(),
                warehouse.getName(),
                warehouse.getWarehouseCapacityKg(),
                user.getId(),
                warehouse.getWarehouseAddress().getAddressId()
        ));

    }

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public WarehouseResponse addWarehouse(WarehouseRequest request, UUID id){
        UserModel user = userModelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        WarehouseAddress address = new WarehouseAddress();
        applyAddress(address, request);

        Warehouse warehouse = new Warehouse();
        warehouse.setName(request.name());
        warehouse.setWarehouseCapacityKg(request.warehouseCapacityKg());
        warehouse.setUser(user);
        warehouse.setWarehouseAddress(address);
        address.setWarehouse(warehouse);

        return WarehouseResponse.from(warehouseRepository.save(warehouse));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public WarehouseResponse updateWarehouse(WarehouseRequest request, UUID id){
         userModelRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));

        Warehouse warehouse = warehouseRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: " + id));

        warehouse.setName(request.name());
        warehouse.setWarehouseCapacityKg(request.warehouseCapacityKg());
        applyAddress(warehouse.getWarehouseAddress(), request);

        return WarehouseResponse.from(warehouseRepository.save(warehouse));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public Void delete(UUID id) {
        if (!warehouseRepository.existsById(id)) throw new EntityNotFoundException("Warehouse not found: " + id);
        warehouseRepository.deleteById(id);
        return null;
    }


    private void applyAddress(WarehouseAddress address, WarehouseRequest request) {
        address.setAddressLine1(request.addressLine1());
        address.setAddressLine2(request.addressLine2());
        address.setCity(request.city());
        address.setStateProvince(request.stateProvince());
        address.setCountry(request.country());
        address.setPostalCode(request.postalCode());
    }

}
