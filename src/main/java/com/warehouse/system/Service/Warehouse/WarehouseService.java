package com.warehouse.system.Service.Warehouse;

import com.warehouse.system.DTO.Request.WarehouseRequest;
import com.warehouse.system.DTO.Response.AddressResponse;
import com.warehouse.system.DTO.Response.ScrollResponse;
import com.warehouse.system.DTO.Response.WarehouseResponse;
import com.warehouse.system.DTO.StoredUpload;
import com.warehouse.system.Exception.InvalidCredentials;
import com.warehouse.system.Model.*;
import com.warehouse.system.Repository.UserModelRepository;
import com.warehouse.system.Repository.WarehouseImageRepository;
import com.warehouse.system.Repository.WarehouseRepository;
import com.warehouse.system.Service.File.FileStorageService;
import com.warehouse.system.Utils.FileUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final UserModelRepository userModelRepository;
    private final WarehouseImageRepository warehouseImageRepository;
    private final FileStorageService storageService;
    private final FileUtils fileUtils;
    @Value("${app.upload.dir}")
    private String imageBaseUrl;

    @Transactional(readOnly = true)
    public ScrollResponse<WarehouseResponse> getAllWarehouse(UUID cursor, int size, UUID userId) {
        UUID lastId = cursor != null ? cursor : new UUID(0L, 0L);
        Slice<Warehouse> slice = warehouseRepository
                .findUnsavedFeed(lastId, userId, PageRequest.of(0, size));

        List<Warehouse> warehouses = slice.getContent();

        Map<UUID, List<WarehouseImage>> imageUrlsByWarehouse = listByWarehouseIds(warehouses.stream().map(Warehouse::getWarehouseId).toList());

        List<WarehouseResponse> items = warehouses.stream()
                .map(w -> WarehouseResponse.from(
                        w,
                        false,
                        imageUrlsByWarehouse.getOrDefault(w.getWarehouseId(), List.of())
                                .stream().map(img -> "/api/warehouse/images/" + img.getId())
                                .toList()
                ))
                .toList();

        UUID nextCursor = slice.hasNext()
                ? warehouses.getLast().getWarehouseId()
                : null;

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
                false,
                null
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
                false,
                null
        ));

    }

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public WarehouseResponse addWarehouse(WarehouseRequest request,List<MultipartFile> files, UUID userId) throws IOException {
        UserModel user = userModelRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        WarehouseAddress address = new WarehouseAddress();
        applyAddress(address, request);

        Warehouse warehouse = new Warehouse();
        warehouse.setName(request.name());
        warehouse.setWarehouseCapacityKg(request.warehouseCapacityKg());
        warehouse.setUser(user);
        warehouse.setWarehouseAddress(address);
        address.setWarehouse(warehouse);

        Warehouse saved = warehouseRepository.save(warehouse);

        List<WarehouseImage> images = saveImages(saved, files);

        return WarehouseResponse.from(saved, false, fileUtils.toUrls(images));

    }

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public WarehouseResponse updateWarehouse(UUID warehouseId,
                                             WarehouseRequest request,
                                             List<MultipartFile> files,
                                             List<UUID> removeImageIds,
                                             UUID userId) throws IOException {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: " + warehouseId));

        if (!warehouse.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this warehouse");
        }

        warehouse.setName(request.name());
        warehouse.setWarehouseCapacityKg(request.warehouseCapacityKg());
        applyAddress(warehouse.getWarehouseAddress(), request);
        Warehouse saved = warehouseRepository.save(warehouse);

        List<String> filesToDelete = new ArrayList<>();
        if (removeImageIds != null && !removeImageIds.isEmpty()) {
            List<WarehouseImage> toRemove = warehouseImageRepository
                    .findByIdInAndWarehouse_WarehouseId(removeImageIds, warehouseId);
            toRemove.forEach(img -> filesToDelete.add(img.getStoredFileName()));
            warehouseImageRepository.deleteAll(toRemove);
        }

        saveImages(saved, files);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileUtils.deleteQuietly(filesToDelete);
            }
        });

        List<WarehouseImage> all = warehouseImageRepository
                .findByWarehouse_WarehouseIdInOrderByUploadedAtAsc(List.of(warehouseId));

        return WarehouseResponse.from(saved,false,fileUtils.toUrls(all));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public Void delete(UUID userId,UUID warehouseId) {
        userModelRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (!warehouseRepository.existsById(warehouseId)) throw new EntityNotFoundException("Warehouse not found: " + warehouseId);
        warehouseRepository.deleteById(warehouseId);
        return null;
    }

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public List<WarehouseImage> upload(UUID warehouseId, List<MultipartFile> files) throws IOException {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));

        List<WarehouseImage> saved = new ArrayList<>();
        for (MultipartFile file : files) {
            FileStorageService.StoredFile storedName = storageService.store(file);

            WarehouseImage img = new WarehouseImage();
            img.setStoredFileName(storedName.storedName());
            img.setOriginalFileName(StringUtils.cleanPath(
                    Objects.requireNonNullElse(file.getOriginalFilename(), "image")));
            img.setContentType(storedName.contentType());
            img.setSizeBytes(file.getSize());
            img.setWarehouse(warehouse);
            saved.add(warehouseImageRepository.save(img));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<WarehouseImage> list(UUID warehouseId) {
        return warehouseImageRepository.findByWarehouse_WarehouseId(warehouseId);
    }

    @Transactional(readOnly = true)
    public WarehouseImage get(UUID imageId) {
        return warehouseImageRepository.findById(imageId).orElseThrow(() -> new EntityNotFoundException("Image not found"));
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<WarehouseImage>> listByWarehouseIds(Collection<UUID> warehouseIds) {
        if (warehouseIds.isEmpty()) return Map.of();

        return warehouseImageRepository
                .findByWarehouse_WarehouseIdInOrderByUploadedAtAsc(warehouseIds).stream()
                .collect(Collectors.groupingBy(wi -> wi.getWarehouse().getWarehouseId()));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public void deleteImage(UUID imageId) throws IOException {
        WarehouseImage img = get(imageId);
        warehouseImageRepository.delete(img);
        storageService.delete(img.getStoredFileName());
    }

    private void applyAddress(WarehouseAddress address, WarehouseRequest request) {
        address.setAddressLine1(request.addressLine1());
        address.setAddressLine2(request.addressLine2());
        address.setCity(request.city());
        address.setStateProvince(request.stateProvince());
        address.setCountry(request.country());
        address.setPostalCode(request.postalCode());
    }

    private List<WarehouseImage> saveImages(Warehouse warehouse, List<MultipartFile> files) throws IOException {
        List<StoredUpload> uploads = fileUtils.storeFiles(files);
        List<WarehouseImage> images = uploads.stream()
                .map(upload -> {
                    var file = upload.originalFile();
                    var stored = upload.storedFile();

                    WarehouseImage image = new WarehouseImage();
                    image.setStoredFileName(stored.storedName());
                    image.setOriginalFileName(file.getOriginalFilename());
                    image.setContentType(stored.contentType());
                    image.setSizeBytes(file.getSize());
                    image.setWarehouse(warehouse);

                    return image;
                }).toList();
        try {
            return warehouseImageRepository.saveAll(images);
        } catch (RuntimeException e) {
            fileUtils.deleteQuietly(
                    uploads.stream()
                            .map(upload -> upload.storedFile().storedName())
                            .toList()
            );
            throw e;
        }
    }

}
