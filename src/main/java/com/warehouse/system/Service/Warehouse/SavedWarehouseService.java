package com.warehouse.system.Service.Warehouse;


import com.warehouse.system.DTO.Response.ScrollResponse;
import com.warehouse.system.DTO.Response.WarehouseResponse;
import com.warehouse.system.Model.SavedWarehouses;
import com.warehouse.system.Model.Warehouse;
import com.warehouse.system.Model.WarehouseImage;
import com.warehouse.system.Repository.SavedWarehouseRepository;
import com.warehouse.system.Repository.UserModelRepository;
import com.warehouse.system.Repository.WarehouseImageRepository;
import com.warehouse.system.Repository.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SavedWarehouseService {

    private final SavedWarehouseRepository savedRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserModelRepository userModelRepository;
    private final WarehouseImageRepository warehouseImageRepository;

    @Transactional
    public void save(UUID userId, UUID warehouseId) {
        if (savedRepository.existsByUser_IdAndWarehouse_WarehouseId(userId, warehouseId)) {
            return;
        }

        if (!warehouseRepository.existsById(warehouseId)) {
            throw new EntityNotFoundException("Warehouse not found: " + warehouseId);
        }

        SavedWarehouses saved = new SavedWarehouses();
        saved.setUser(userModelRepository.getReferenceById(userId));
        saved.setWarehouse(warehouseRepository.getReferenceById(warehouseId));
        savedRepository.save(saved);
    }

    @Transactional
    public void unsave(UUID userId, UUID warehouseId) {
        savedRepository.deleteByUserIdAndWarehouse_WarehouseId(userId, warehouseId);
    }

    @Transactional(readOnly = true)
    public ScrollResponse<WarehouseResponse> getSaved(UUID userId, UUID cursor, int size) {
        UUID lastId = cursor != null ? cursor : new UUID(0L, 0L);

        Slice<Warehouse> slice = savedRepository
                .findSavedByUser(userId,lastId, PageRequest.of(0, size));

        List<Warehouse> warehouses = slice.getContent();
        Map<UUID, List<WarehouseImage>> imageUrlsByWarehouse = listByWarehouseIds(warehouses.stream().map(Warehouse::getWarehouseId).toList());

        List<WarehouseResponse> items = warehouses.stream().map(w -> WarehouseResponse.from(
                w,
                true,
                imageUrlsByWarehouse.getOrDefault(w.getWarehouseId(), List.of())
                        .stream().map(img -> "/api/warehouse/images/" + img.getId())
                        .toList()
                ))
                .toList();

        UUID nextCursor = slice.hasNext() ? slice.getContent().getLast().getWarehouseId() : null;

        return new ScrollResponse<>(items, nextCursor, slice.hasNext());
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<WarehouseImage>> listByWarehouseIds(Collection<UUID> warehouseIds) {
        if (warehouseIds.isEmpty()) return Map.of();

        return warehouseImageRepository.findByWarehouse_WarehouseIdInOrderByUploadedAtAsc(warehouseIds).stream()
                .collect(Collectors.groupingBy(wi -> wi.getWarehouse().getWarehouseId()));
    }


}
