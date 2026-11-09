package com.warehouse.system.Service.Booking;

import com.warehouse.system.DTO.BookingRental.OwnerBookingStats;
import com.warehouse.system.DTO.Capacity;
import com.warehouse.system.DTO.Interface.StorageSummary;
import com.warehouse.system.DTO.Interface.WarehouseSummary;
import com.warehouse.system.DTO.OwnerBookingResponse;
import com.warehouse.system.DTO.Request.CreateHoldRequest;
import com.warehouse.system.DTO.Response.AddressResponse;
import com.warehouse.system.DTO.Response.BookingDetailResponse;
import com.warehouse.system.DTO.Response.StorageResponse;
import com.warehouse.system.DTO.Response.StorageUsage;
import com.warehouse.system.Enums.BookingStatus;
import com.warehouse.system.Exception.BookingException;
import com.warehouse.system.Model.*;
import com.warehouse.system.Repository.StorageBookingRepository;
import com.warehouse.system.Repository.StorageRepository;
import com.warehouse.system.Repository.UserModelRepository;
import com.warehouse.system.Repository.WarehouseImageRepository;
import com.warehouse.system.Service.Lease.LeaseSnapshotService;
import com.warehouse.system.Service.Warehouse.WarehouseService;
import com.warehouse.system.Utils.Computations;
import com.warehouse.system.Utils.FileUtils;
import jakarta.persistence.LockTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageBookingService {

    private final StorageBookingRepository storageBookingRepository;
    private final UserModelRepository userModelRepository;
    private final StorageRepository storageRepository;
    private final WarehouseImageRepository warehouseImageRepository;
    private final LeaseSnapshotService leaseSnapshotService;
    private final FileUtils fileUtils;
    private final Computations computations;
    private final Clock clock;

    private static final Set<BookingStatus> HIDDEN_FROM_OWNER = EnumSet.of(BookingStatus.PENDING_TERMS);

    @Value("${booking.request-expiry-hours:48}")
    private long requestExpiryHours;
    @Value("${booking.max-pending-per-user:3}")
    private int maxPendingPerUser;
    @Value("${booking.min-days:1}")
    private long minDays;
    @Value("${booking.max-days:365}")
    private long maxDays;
    @Value("${booking.max-advance-days:365}")
    private long maxAdvanceDays;

    @Transactional
    public StorageBooking createBooking(UUID userId, CreateHoldRequest req) {
        LocalDateTime now = LocalDateTime.now(clock);
        validate(req, now.toLocalDate());

        LocalDateTime start = req.startDate().atStartOfDay();
        LocalDateTime end = req.endDate().atStartOfDay();

        UserModel user = userModelRepository.findById(userId)
                .orElseThrow(() -> new BookingException.NotFound("User not found"));

        Storage storage = lockStorage(req.storageId());

        if (storage.getWarehouse().getUser().getId().equals(userId)) {
            throw new BookingException.Forbidden("You can't book your own storage");
        }
        if (!storage.isListed()) {
            throw new BookingException.Conflict("This storage is not available for booking");
        }
        if (storage.getPricePerCubicMeter() == null) {
            throw new BookingException.Validation("This storage has no price set");
        }
        if (req.rentedWeightKg() > storage.getMaxWeight()) {
            throw new BookingException.Validation(
                    "Weight exceeds this storage's limit of " + storage.getMaxWeight() + " kg");
        }
        if (req.rentedVolumeCubicMeter() > storage.getMaxVolumeCubicMeter()) {
            throw new BookingException.Validation(
                    "Volume exceeds this storage's limit of " + storage.getMaxVolumeCubicMeter() + " m³");
        }

        storageBookingRepository.expireHoldsForStorage(
                storage.getStorageId(), BookingStatus.EXPIRED, BookingStatus.PENDING_TERMS, now);

        Capacity cap = remainingCapacity(storage, start, end, now);
        if (req.rentedVolumeCubicMeter() > cap.availableVolume()
                || req.rentedWeightKg() > cap.availableWeight()) {
            throw new BookingException.Conflict(String.format(
                    "Not enough space for those dates. Available: %.2f m³ and %.2f kg",
                    cap.availableVolume(), cap.availableWeight()));
        }

        if (storageBookingRepository.countActiveHolds(userId, BookingStatus.PENDING_TERMS, now) >= maxPendingPerUser) {
            throw new BookingException.LimitExceeded(
                    "You can only have " + maxPendingPerUser + " pending requests at a time");
        }

        BigDecimal totalPrice = computations.billableMonths(start, end,storage.getPricePerCubicMeter(),req.rentedVolumeCubicMeter());

        StorageBooking booking = new StorageBooking();
        booking.setUser(user);
        booking.setStorage(storage);
        booking.setStartDate(start);
        booking.setEndDate(end);
        booking.setUnitNumber(storage.getBlockName() + "-" + String.format("%04d", storage.nextUnitNumber()));
        booking.setRentedVolumeCubicMeter(req.rentedVolumeCubicMeter());
        booking.setRentedWeightKg(req.rentedWeightKg());
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.PENDING_TERMS);
        booking.setHoldExpiresAt(now.plusHours(requestExpiryHours));
        StorageBooking saved = storageBookingRepository.saveAndFlush(booking);
        leaseSnapshotService.createForBooking(saved);
        return saved;
    }

    @Transactional
    public StorageBooking cancel(UUID orderId, UUID userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        StorageBooking booking = lockBooking(orderId);
        assertOwner(booking, userId);

        switch (booking.getStatus()) {
            case CANCELLED, EXPIRED, REJECTED -> { return booking; }
            case PENDING_TERMS -> { /* always cancellable */ }
            case CONFIRMED -> {
                if (!booking.getStartDate().isAfter(now)) {
                    throw new BookingException.InvalidState("Booking has already started");
                }
            }
            default -> throw new BookingException.InvalidState("Cannot cancel a " + booking.getStatus() + " booking");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setHoldExpiresAt(null);
        return booking;
    }

    @Transactional(readOnly = true)
    public List<BookingDetailResponse> getBookingsByUser(UUID userId) {
        List<StorageBooking> bookings = storageBookingRepository.findByUser_IdWithDetails(userId);
        if (bookings.isEmpty()) return List.of();

        List<UUID> warehouseIds = bookings.stream()
                .map(b -> b.getStorage().getWarehouse().getWarehouseId())
                .distinct()
                .toList();

        Map<UUID, List<String>> imgsByWarehouse = warehouseImageRepository
                .findByWarehouse_WarehouseIdInOrderByUploadedAtAsc(warehouseIds).stream()
                .collect(Collectors.groupingBy(
                        i -> i.getWarehouse().getWarehouseId(),
                        Collectors.collectingAndThen(Collectors.toList(), fileUtils::toUrls)));

        return bookings.stream().map(b -> {
            Storage s = b.getStorage();
            Warehouse w = s.getWarehouse();
            return new BookingDetailResponse(
                    b.getOrderId(),
                    b.getUnitNumber(),
                    b.getStartDate(),
                    b.getEndDate(),
                    b.getStatus(),
                    b.getRentedWeightKg(),
                    b.getRentedVolumeCubicMeter(),
                    new StorageSummary(
                            s.getStorageId(),
                            s.getBlockName(),
                            s.getSection(),
                            s.getMaxWeight(),
                            s.getPricePerCubicMeter(),
                            s.getMaxVolumeCubicMeter(),
                            s.getMaterialType()
                    ),
                    new WarehouseSummary(
                            w.getWarehouseId(),
                            w.getName(),
                            w.getWarehouseCapacityKg(),
                            AddressResponse.from(w.getWarehouseAddress()),
                            imgsByWarehouse.getOrDefault(w.getWarehouseId(), List.of())
                    )
            );
        }).toList();
    }

    @Transactional
    public StorageBooking approve(UUID orderId, UUID sellerId) {
        return decide(orderId, sellerId, true);
    }

    @Transactional
    public StorageBooking reject(UUID orderId, UUID sellerId) {
        return decide(orderId, sellerId, false);
    }

    private StorageBooking decide(UUID orderId, UUID sellerId, boolean approve) {
        LocalDateTime now = LocalDateTime.now(clock);

        UUID storageId = storageBookingRepository.findById(orderId)
                .orElseThrow(() -> new BookingException.NotFound("Booking not found"))
                .getStorage().getStorageId();
        Storage storage = lockStorage(storageId);
        StorageBooking booking = lockBooking(orderId);

        if (!storage.getWarehouse().getUser().getId().equals(sellerId)) {
            throw new BookingException.NotFound("Booking not found");
        }
        if (booking.getStatus() != BookingStatus.PENDING_TERMS) {
            throw new BookingException.InvalidState("Booking is " + booking.getStatus());
        }
        if (!booking.getHoldExpiresAt().isAfter(now)) {
            throw new BookingException.HoldExpired("This request has expired");
        }

        if (approve) {
            booking.setUnitNumber(String.format("U-%04d", storage.nextUnitNumber()));
            booking.setStatus(BookingStatus.CONFIRMED);
        } else {
            booking.setStatus(BookingStatus.REJECTED);
        }
        booking.setHoldExpiresAt(null);
        return booking;
    }

    @Transactional(readOnly = true)
    public List<StorageBooking> viewStorageBookings(UUID storageId, UUID userId) {
        Storage storage = storageRepository.findById(storageId)
                .orElseThrow(() -> new BookingException.NotFound("Storage not found"));

        if (!storage.getWarehouse().getUser().getId().equals(userId)) {
            throw new BookingException.Forbidden("You can only view bookings for your own storage");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        return storageBookingRepository.findAllByStorageId(storageId).stream()
                .filter(b -> !(b.getStatus() == BookingStatus.PENDING_TERMS
                        && b.getHoldExpiresAt() != null
                        && !b.getHoldExpiresAt().isAfter(now)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<OwnerBookingResponse> getBookingsByOwner(UUID ownerId, int page, int size, String search, BookingStatus status) {
        String like = (search == null || search.isBlank()) ? "%" : "%" + search.trim().toLowerCase() + "%";
        Pageable pr = PageRequest.of(page, size, Sort.by(Sort.Order.desc("startDate"), Sort.Order.desc("orderId")));

        if (status != null && HIDDEN_FROM_OWNER.contains(status)) return Page.empty(pr);

        Collection<BookingStatus> statuses = status != null
                ? List.of(status)
                : EnumSet.complementOf(EnumSet.copyOf(HIDDEN_FROM_OWNER));

        return storageBookingRepository.findOwnerPage(ownerId, statuses, like, pr)
                .map(OwnerBookingResponse::from);
    }

    @Transactional(readOnly = true)
    public OwnerBookingStats getOwnerStats(UUID ownerId) {
        Collection<BookingStatus> visible = EnumSet.complementOf(EnumSet.copyOf(HIDDEN_FROM_OWNER));
        LocalDateTime now = LocalDateTime.now();
        return storageBookingRepository.findOwnerStats(
                ownerId, visible,
                BookingStatus.CONFIRMED, BookingStatus.PENDING_OWNER,
                now, now.plusDays(7));
    }

    @Transactional
    public int expireStaleRequests() {
        int n = storageBookingRepository.expireHolds(
                BookingStatus.EXPIRED, BookingStatus.PENDING_TERMS, LocalDateTime.now(clock));
        if (n > 0) log.info("Expired {} stale booking requests", n);
        return n;
    }

    @Transactional(readOnly = true)
    public Capacity getRemainingCapacity(UUID storageId, LocalDate startDate, LocalDate endDate) {
        Storage s = storageRepository.findById(storageId)
                .orElseThrow(() -> new BookingException.NotFound("Storage not found"));
        if (!s.isListed()) return new Capacity(0, 0);
        return remainingCapacity(s, startDate.atStartOfDay(), endDate.atStartOfDay(), LocalDateTime.now(clock));
    }

    private Capacity remainingCapacity(Storage s, LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        StorageUsage used = storageBookingRepository.sumOverlappingUsage(
                s.getStorageId(), start, end, now, BookingStatus.CONFIRMED, BookingStatus.PENDING_TERMS);

        return new Capacity(
                Math.max(0, s.getMaxVolumeCubicMeter() - used.volume()),
                Math.max(0, s.getMaxWeight() - used.weight()));
    }

    private void validate(CreateHoldRequest req, LocalDate now) {
        LocalDate start = req.startDate();
        LocalDate end = req.endDate();

        if (!end.isAfter(start)) {
            throw new BookingException.Validation("End date must be after start date");
        }
        if (start.isBefore(now)) {
            throw new BookingException.Validation("Start date cannot be in the past");
        }
        if (start.isAfter(now.plusDays(maxAdvanceDays))) {
            throw new BookingException.Validation("Cannot book more than " + maxAdvanceDays + " days ahead");
        }
        long days = ChronoUnit.DAYS.between(start, end);
        if (days < minDays) {
            throw new BookingException.Validation("Minimum booking length is " + minDays + " day(s)");
        }
        if (days > maxDays) {
            throw new BookingException.Validation("Maximum booking length is " + maxDays + " days");
        }
    }

    private Storage lockStorage(UUID storageId) {
        try {
            return storageRepository.findByIdForUpdate(storageId)
                    .orElseThrow(() -> new BookingException.NotFound("Storage not found"));
        } catch (PessimisticLockingFailureException | LockTimeoutException e) {
            throw new BookingException.Busy("System is busy, please retry");
        }
    }

    private StorageBooking lockBooking(UUID orderId) {
        try {
            return storageBookingRepository.findByIdForUpdate(orderId)
                    .orElseThrow(() -> new BookingException.NotFound("Booking not found"));
        } catch (PessimisticLockingFailureException | LockTimeoutException e) {
            throw new BookingException.Busy("System is busy, please retry");
        }
    }

    private void assertOwner(StorageBooking booking, UUID userId) {
        if (!booking.getUser().getId().equals(userId)) {
            throw new BookingException.NotFound("Booking not found");
        }
    }

}
