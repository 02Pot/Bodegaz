package com.warehouse.system.Service;

import com.warehouse.system.DTO.Request.StorageRequest;
import com.warehouse.system.DTO.Response.StorageResponse;
import com.warehouse.system.Model.Storage;
import com.warehouse.system.Model.StorageType;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Model.Warehouse;
import com.warehouse.system.Repository.StorageRepository;
import com.warehouse.system.Repository.UserModelRepository;
import com.warehouse.system.Repository.WarehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StorageService {

    private final WarehouseRepository warehouseRepository;
    private final StorageRepository storageRepository;
    private final UserModelRepository userModelRepository;

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public StorageResponse addStorage(StorageRequest request, UUID userId,UUID warehouseId){
        UserModel user = userModelRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse doesnt exist"));

        StorageType storageInfo = new StorageType();

        Storage storage = new Storage();

        storage.setBlockName(request.blockName());
        storage.setSection(request.section());
        storage.setMaxWeight(request.maxWeight());

        storage.setWarehouse(warehouse);
        storage.setSellerId(user.getId());

        storageInfo.setLengthMeters(request.lengthMeters());
        storageInfo.setBreadthMeters(request.breadthMeters());
        storageInfo.setHeightMeters(request.heightMeters());
        storageInfo.setCapacityWeight(request.capacityWeight());
        storageInfo.setUnitsAvailable(request.unitsAvailable());

        storage.setStorageType(storageInfo);

        return StorageResponse.from(storageRepository.save(storage));
    }

    public Page<StorageResponse> getAvailableWarehouseStorage(UUID warehouseId, UUID userId, int page, int size){
        userModelRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(page,size);
        Page<Storage> storages = storageRepository.findAvailableUnitsInWarehouse(
                warehouseId, pageable
        );

        return storages.map(storage -> new StorageResponse(
                storage.getBlockName(),
                storage.getSection(),
                storage.getMaxWeight(),
                storage.isAvailable(),
                storage.getMaterialType(),
                warehouseId,
                userId
        ));

    }

}
