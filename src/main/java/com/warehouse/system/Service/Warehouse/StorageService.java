package com.warehouse.system.Service.Warehouse;

import com.warehouse.system.DTO.Request.StorageRequest;
import com.warehouse.system.DTO.Request.StorageUpdateRequest;
import com.warehouse.system.DTO.Response.StorageResponse;
import com.warehouse.system.DTO.Response.StorageUsage;
import com.warehouse.system.Enums.BookingStatus;
import com.warehouse.system.Exception.BookingException;
import com.warehouse.system.Model.*;
import com.warehouse.system.Repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Transactional
public class StorageService {

    private final WarehouseRepository warehouseRepository;
    private final StorageRepository storageRepository;
    private final UserModelRepository userModelRepository;
    private final StorageTypeRepository storageTypeRepository;
    private final StorageBookingRepository storageBookingRepository;
    private final Clock clock;

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
            s.setListed(true);
            s.setPricePerCubicMeter(request.pricePerCubicMeter());
            s.setMaxVolumeCubicMeter(request.maxVolumeCubicMeter());
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
    public StorageResponse updateStorage(UUID storageId, StorageUpdateRequest request,UUID userId) {
        Storage storage = storageRepository.findByIdForUpdate(storageId)
                .orElseThrow(() -> new EntityNotFoundException("Storage not found: " + storageId));

        if(!storage.getWarehouse().getUser().getId().equals(userId)){
            throw new AccessDeniedException("You do not own this storage");
        }

        if (request == null) {
            return StorageResponse.from(storage);
        }

        LocalDateTime now = LocalDateTime.now(clock);

        StorageUsage committed = storageBookingRepository.sumOverlappingUsage(
                storage.getStorageId(), now, now.plusYears(50), now,
                BookingStatus.CONFIRMED, BookingStatus.HELD);
        boolean hasActiveBookings = committed.volume() > 0 || committed.weight() > 0;

        if (request.maxWeight() != null && request.maxWeight() != storage.getMaxWeight()) {
            double newWeight = request.maxWeight();

            double typeCap = storage.getStorageType().getCapacityWeight();
            if (newWeight > typeCap) {
                throw new BookingException.Validation(
                        "This storage type holds up to " + typeCap + " kg per unit");
            }

            double others = storageRepository.sumMaxWeightOfOthers(storage.getWarehouse(), storageId);
            double warehouseCap = storage.getWarehouse().getWarehouseCapacityKg();
            if (others + newWeight > warehouseCap) {
                throw new BookingException.Validation(
                        "Warehouse capacity exceeded: only " + (warehouseCap - others) + " kg is left for this storage");
            }

            if (newWeight < committed.weight()) {
                throw new BookingException.Conflict(
                        "Active bookings already use " + committed.weight() + " kg, so the limit can't go below that");
            }
            storage.setMaxWeight(newWeight);
        }

        if (request.maxVolumeCubicMeter() != null
                && request.maxVolumeCubicMeter() != storage.getMaxVolumeCubicMeter()) {
            double newVolume = request.maxVolumeCubicMeter();

            if (newVolume < committed.volume()) {
                throw new BookingException.Conflict(
                        "Active bookings already use " + committed.volume() + " m³, so the limit can't go below that");
            }
            storage.setMaxVolumeCubicMeter(newVolume);
        }

        if (request.materialType() != null && request.materialType() != storage.getMaterialType()) {
            if (hasActiveBookings) {
                throw new BookingException.Conflict(
                        "Material type can't change while there are active or upcoming bookings");
            }
            storage.setMaterialType(request.materialType());
        }

        applyIfPresent(request.blockName() == null ? null : request.blockName().trim(), storage::setBlockName);
        applyIfPresent(request.section() == null ? null : request.section().trim(), storage::setSection);
        applyIfPresent(request.pricePerCubicMeter(), storage::setPricePerCubicMeter);

        return StorageResponse.from(storage);
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


    private <T> void applyIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

}
