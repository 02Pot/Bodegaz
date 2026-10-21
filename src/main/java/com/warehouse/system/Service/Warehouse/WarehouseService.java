package com.warehouse.system.Service.Warehouse;

import com.warehouse.system.DTO.Interface.BookedStats;
import com.warehouse.system.DTO.Interface.StorageStats;
import com.warehouse.system.DTO.Request.WarehouseRequest;
import com.warehouse.system.DTO.Request.WarehouseUpdateRequest;
import com.warehouse.system.DTO.Response.AddressResponse;
import com.warehouse.system.DTO.Response.ScrollResponse;
import com.warehouse.system.DTO.Response.SellerWarehouseResponse;
import com.warehouse.system.DTO.Response.WarehouseResponse;
import com.warehouse.system.DTO.StoredUpload;
import com.warehouse.system.Enums.BookingStatus;
import com.warehouse.system.Exception.InvalidCredentials;
import com.warehouse.system.Model.*;
import com.warehouse.system.Repository.*;
import com.warehouse.system.Service.File.FileStorageService;
import com.warehouse.system.Utils.FileUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final UserModelRepository userModelRepository;
    private final WarehouseImageRepository warehouseImageRepository;
    private final FileStorageService storageService;
    private final FileUtils fileUtils;
    private final SavedWarehouseRepository savedWarehouseRepository;
    private final StorageBookingRepository bookingRepository;
    private final StorageRepository storageRepository;
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

    public WarehouseResponse getByWarehouseId(UUID userId,UUID id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: " + id));

        List<String> imagesByWarehouse = warehouseImageRepository
                .findByWarehouse_WarehouseId(id)
                .stream()
                .map(img -> "/api/warehouse/images/" + img.getId())
                .toList();

        boolean saved = savedWarehouseRepository
                .existsByUser_IdAndWarehouse_WarehouseId(userId, id);

        return WarehouseResponse.from(warehouse, saved, imagesByWarehouse);
    }

    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public List<SellerWarehouseResponse> getMyWarehouse(UUID userId){
        List<Warehouse> warehouses = warehouseRepository.findByOwnerWithAddress(userId);
        if (warehouses.isEmpty()) return List.of();

        List<UUID> ids = warehouses.stream().map(Warehouse::getWarehouseId).toList();

        Map<UUID, StorageStats> stats = storageRepository.statsByOwner(userId).stream()
                .collect(Collectors.toMap(StorageStats::getWarehouseId, Function.identity()));

        Map<UUID, Long> booked = bookingRepository
                .bookedByOwner(userId, List.of(BookingStatus.HELD, BookingStatus.CONFIRMED)).stream()
                .collect(Collectors.toMap(BookedStats::getWarehouseId, BookedStats::getBookedCount));

        Map<UUID, List<WarehouseImage>> imagesByWarehouse = warehouseImageRepository
                .findByWarehouse_WarehouseIdInOrderByUploadedAtAsc(ids)
                .stream()
                .collect(Collectors.groupingBy(img -> img.getWarehouse().getWarehouseId()));

        return warehouses.stream().map(w -> {
            UUID id = w.getWarehouseId();
            StorageStats st = stats.get(id);
            long storageCount = st == null ? 0L : st.getStorageCount();
            double allocatedKg = st == null ? 0.0 : st.getAllocatedKg();

            return new SellerWarehouseResponse(
                    id,
                    w.getName(),
                    w.getWarehouseCapacityKg(),
                    w.getViewCount() == null ? 0L : w.getViewCount(),
                    w.getCreatedAt(),
                    AddressResponse.from(w.getWarehouseAddress()),
                    allocatedKg,
                    storageCount,
                    booked.getOrDefault(id, 0L),
                    storageCount == 0,
                    fileUtils.toImageResponses(imagesByWarehouse.getOrDefault(id, List.of()))
            );
        }).toList();
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
                                             WarehouseUpdateRequest request,
                                             List<MultipartFile> files,
                                             List<UUID> removeImageIds,
                                             UUID userId) throws IOException {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse not found: " + warehouseId));

        if (!warehouse.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not own this warehouse");
        }

        if (request != null) {
            applyIfPresent(request.name(), warehouse::setName);
            applyIfPresent(request.warehouseCapacityKg(), warehouse::setWarehouseCapacityKg);
            applyAddressPatch(warehouse.getWarehouseAddress(), request);
        }
        Warehouse saved = warehouseRepository.save(warehouse);

        List<String> filesToDelete = new ArrayList<>();
        if (removeImageIds != null && !removeImageIds.isEmpty()) {
            List<WarehouseImage> toRemove = warehouseImageRepository
                    .findByIdInAndWarehouse_WarehouseId(removeImageIds, warehouseId);
            toRemove.forEach(img -> filesToDelete.add(img.getStoredFileName()));
            warehouseImageRepository.deleteAll(toRemove);
        }

        if (files != null && !files.isEmpty()) {
            saveImages(saved, files);
        }

        if (!filesToDelete.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileUtils.deleteQuietly(filesToDelete);
                }
            });
        }

        List<WarehouseImage> all = warehouseImageRepository
                .findByWarehouse_WarehouseIdInOrderByUploadedAtAsc(List.of(warehouseId));

        return WarehouseResponse.from(saved, false, fileUtils.toUrls(all));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public Void delete(UUID userId,UUID warehouseId) {
        userModelRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (!warehouseRepository.existsById(warehouseId)) throw new EntityNotFoundException("Warehouse not found: " + warehouseId);
        savedWarehouseRepository.deleteAllByWarehouse_WarehouseId(warehouseId);
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


    private <T> void applyIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private void applyAddressPatch(WarehouseAddress address, WarehouseUpdateRequest r) {
        applyIfPresent(r.addressLine1(), address::setAddressLine1);
        applyIfPresent(r.addressLine2(), address::setAddressLine2);
        applyIfPresent(r.city(), address::setCity);
        applyIfPresent(r.stateProvince(), address::setStateProvince);
        applyIfPresent(r.country(), address::setCountry);
        applyIfPresent(r.postalCode(), address::setPostalCode);
    }

}

