package com.warehouse.system.Service.Warehouse;

import com.warehouse.system.DTO.Request.StorageRequest;
import com.warehouse.system.DTO.Response.StorageResponse;
import com.warehouse.system.Model.Storage;
import com.warehouse.system.Model.StorageType;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Model.Warehouse;
import com.warehouse.system.Repository.StorageRepository;
import com.warehouse.system.Repository.StorageTypeRepository;
import com.warehouse.system.Repository.UserModelRepository;
import com.warehouse.system.Repository.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StorageService {

    private final WarehouseRepository warehouseRepository;
    private final StorageRepository storageRepository;
    private final UserModelRepository userModelRepository;
    private final StorageTypeRepository storageTypeRepository;

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public List<StorageResponse> addStorage(StorageRequest request, UUID userId,UUID warehouseId){
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse doesn't exist"));

        if (!warehouse.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You don't own this warehouse");
        }

        StorageType type = storageTypeRepository.findById(request.storageTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Storage type not found"));

        double maxWeight = request.maxWeight() != null ? request.maxWeight() : type.getCapacityWeight();
        if (maxWeight > type.getCapacityWeight()) {
            throw new IllegalArgumentException("maxWeight exceeds the storage type's capacity");
        }

        int qty = request.quantity() != null ? request.quantity() : 1;

        List<Storage> created = new ArrayList<>();
        for (int i = 1; i <= qty; i++) {
            Storage s = new Storage();
            s.setBlockName(qty == 1 ? request.blockName() : request.blockName() + "-" + i);
            s.setSection(request.section());
            s.setMaxWeight(maxWeight);
            s.setMaterialType(request.materialType());
            s.setStorageType(type);
            warehouse.addStorage(s);
            created.add(s);
        }

        storageRepository.saveAll(created);

        return created.stream().map(StorageResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<StorageResponse> getStorages(UUID warehouseId) {
        return storageRepository.findByWarehouse_WarehouseId(warehouseId)
                .stream().map(StorageResponse::from).toList();
    }

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public void deleteStorage(UUID warehouseId, UUID storageId, UUID currentUserId) {
        Storage storage = storageRepository.findById(storageId)
                .orElseThrow(() -> new EntityNotFoundException("Storage not found"));

        if (!storage.getWarehouse().getWarehouseId().equals(warehouseId)
                || !storage.getWarehouse().getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Not allowed");
        }
        storageRepository.delete(storage);
    }


}
