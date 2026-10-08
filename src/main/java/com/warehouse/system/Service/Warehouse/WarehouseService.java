package com.warehouse.system.Service.Warehouse;

import com.warehouse.system.DTO.Request.WarehouseRequest;
import com.warehouse.system.DTO.Response.AddressResponse;
import com.warehouse.system.DTO.Response.ScrollResponse;
import com.warehouse.system.DTO.Response.WarehouseResponse;
import com.warehouse.system.Exception.InvalidCredentials;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Model.Warehouse;
import com.warehouse.system.Model.WarehouseAddress;
import com.warehouse.system.Repository.SavedWarehouseRepository;
import com.warehouse.system.Repository.UserModelRepository;
import com.warehouse.system.Repository.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final UserModelRepository userModelRepository;
    private final SavedWarehouseRepository savedWarehouseRepository;

    @Transactional(readOnly = true)
    public ScrollResponse<WarehouseResponse> getAllWarehouse(UUID cursor, int size, UUID userId) {
        UUID lastId = cursor != null ? cursor : new UUID(0L, 0L);

        Slice<Warehouse> slice = warehouseRepository
                .findByWarehouseIdGreaterThanOrderByWarehouseIdAsc(lastId, PageRequest.of(0, size));

        List<UUID> ids = slice.getContent().stream().map(Warehouse::getWarehouseId).toList();

        Set<UUID> savedIds = ids.isEmpty()
                ? Set.of()
                : savedWarehouseRepository.findByUserIdAndWarehouse_WarehouseIdIn(userId, ids);

        List<WarehouseResponse> items = slice.getContent().stream()
                .map(w -> WarehouseResponse.from(w, savedIds.contains(w.getWarehouseId())))
                .toList();

        UUID nextCursor = slice.hasNext() ? slice.getContent().getLast().getWarehouseId() : null;
        return new ScrollResponse<>(items, nextCursor, slice.hasNext());
    }

    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public WarehouseResponse getById(UUID id) {
        return WarehouseResponse.from(warehouseRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Warehouse not found: " + id)));
    }

    @Transactional
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
                warehouse.getViewCount(),
                warehouse.getCreatedAt(),
                user.getId(),
                AddressResponse.from(warehouse.getWarehouseAddress()),
                false
        ));
    }

    @Transactional
    public Page<WarehouseResponse> getByNewest(UUID id,int size){
        UserModel user = userModelRepository.findById(id)
                .orElseThrow(InvalidCredentials::new);

        Pageable pageable = PageRequest.of(0,size);

        Page<Warehouse> newest = warehouseRepository.findNewest(pageable);

        return newest.map(warehouse -> new WarehouseResponse(
                warehouse.getWarehouseId(),
                warehouse.getName(),
                warehouse.getWarehouseCapacityKg(),
                warehouse.getViewCount(),
                warehouse.getCreatedAt(),
                user.getId(),
                AddressResponse.from(warehouse.getWarehouseAddress()),
                false
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
    public WarehouseResponse updateWarehouse(UUID warehouseId, WarehouseRequest request, UUID userId) {
         userModelRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: " + warehouseId));

        warehouse.setName(request.name());
        warehouse.setWarehouseCapacityKg(request.warehouseCapacityKg());
        applyAddress(warehouse.getWarehouseAddress(), request);

        return WarehouseResponse.from(warehouseRepository.save(warehouse));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public Void delete(UUID userId,UUID warehouseId) {
        userModelRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (!warehouseRepository.existsById(warehouseId)) throw new EntityNotFoundException("Warehouse not found: " + warehouseId);
        warehouseRepository.deleteById(warehouseId);
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
