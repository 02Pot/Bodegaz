package com.warehouse.system.Service.Warehouse;


import com.warehouse.system.DTO.Response.ScrollResponse;
import com.warehouse.system.DTO.Response.WarehouseResponse;
import com.warehouse.system.Model.SavedWarehouses;
import com.warehouse.system.Model.Warehouse;
import com.warehouse.system.Repository.SavedWarehouseRepository;
import com.warehouse.system.Repository.UserModelRepository;
import com.warehouse.system.Repository.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SavedWarehouseService {

    private final SavedWarehouseRepository savedRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserModelRepository userModelRepository;

//    PUT    /warehouses/{id}/save     -> save (idempotent)
//    DELETE /warehouses/{id}/save     -> unsave
//    GET    /me/saved-warehouses      -> the user's saved list (cursor-paginated like your feed)

    @Transactional(readOnly = true)
    public ScrollResponse<WarehouseResponse> getAllWarehouse(UUID cursor, int size) {
        UUID lastId = cursor != null ? cursor : new UUID(0L,0L);
        Pageable pageable = PageRequest.of(0, size);
        Slice<Warehouse> slice = warehouseRepository.findByWarehouseIdGreaterThanOrderByWarehouseIdAsc(lastId,pageable);

        List<WarehouseResponse> items = slice.getContent().stream()
                .map(WarehouseResponse::from)
                .toList();

        UUID nextCursor = slice.hasContent()
                ? slice.getContent().getLast().getWarehouseId() : null;

        return new ScrollResponse<>(items,nextCursor,slice.hasNext());
    }

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

        Slice<SavedWarehouses> slice = savedRepository
                .findByUser_IdAndIdGreaterThanOrderByIdAsc(userId, lastId, PageRequest.of(0, size));

        List<WarehouseResponse> items = slice.getContent().stream()
                .map(s -> WarehouseResponse.from(s.getWarehouse(), true))
                .toList();

        UUID nextCursor = slice.hasNext()
                ? slice.getContent().getLast().getId()
                : null;

        return new ScrollResponse<>(items, nextCursor, slice.hasNext());
    }


}
