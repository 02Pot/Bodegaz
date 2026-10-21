package com.warehouse.system.Service.Booking;

import com.warehouse.system.DTO.Request.CreateHoldRequest;
import com.warehouse.system.DTO.Response.StorageUsage;
import com.warehouse.system.Enums.BookingStatus;
import com.warehouse.system.Exception.BookingException;
import com.warehouse.system.Model.Storage;
import com.warehouse.system.Model.StorageBooking;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Repository.StorageBookingRepository;
import com.warehouse.system.Repository.StorageRepository;
import com.warehouse.system.Repository.UserModelRepository;
import jakarta.persistence.LockTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageBookingService {

    private final StorageBookingRepository storageBookingRepository;
    private final UserModelRepository userModelRepository;
    private final StorageRepository storageRepository;
    private final Clock clock;

    @Value("${booking.hold-minutes:10}")
    private long holdMinutes;
    @Value("${booking.max-active-holds-per-user:3}")
    private int maxActiveHoldsPerUser;
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
        if (!storage.isAvailable()) {
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

        storageBookingRepository.expireHoldsForStorage(storage.getStorageId(), BookingStatus.EXPIRED, BookingStatus.HELD, now);

        if (storageBookingRepository.countActiveHolds(userId, BookingStatus.HELD, now) >= maxActiveHoldsPerUser) {
            throw new BookingException.LimitExceeded(
                    "You can only have " + maxActiveHoldsPerUser + " active holds at a time");
        }

        StorageUsage used = storageBookingRepository.sumOverlappingUsage(storage.getStorageId(), start, end, now, BookingStatus.CONFIRMED, BookingStatus.HELD);
        if (used.volume() + req.rentedVolumeCubicMeter() > storage.getMaxVolumeCubicMeter()
                || used.weight() + req.rentedWeightKg() > storage.getMaxWeight()) {
            throw new BookingException.Conflict("Not enough space available for those dates");
        }
        long months = billableMonths(start, end);
        BigDecimal totalPrice = storage.getPricePerCubicMeter()
                .multiply(BigDecimal.valueOf(req.rentedVolumeCubicMeter()))
                .multiply(BigDecimal.valueOf(months))
                .setScale(2, RoundingMode.HALF_UP);

        StorageBooking booking = new StorageBooking();
        booking.setUser(user);
        booking.setStorage(storage);
        booking.setStartDate(start);
        booking.setEndDate(end);
        booking.setRentedVolumeCubicMeter(req.rentedVolumeCubicMeter());
        booking.setRentedWeightKg(req.rentedWeightKg());
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.HELD);
        booking.setHoldExpiresAt(now.plusMinutes(holdMinutes));

        return storageBookingRepository.saveAndFlush(booking);
    }

    @Transactional
    public StorageBooking confirmBooking(UUID orderId, UUID userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        StorageBooking booking = lockBooking(orderId);
        assertOwner(booking, userId);

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return booking;
        }
        if (booking.getStatus() != BookingStatus.HELD) {
            throw new BookingException.InvalidState("Booking is " + booking.getStatus() + ", cannot confirm");
        }
        if (!booking.getHoldExpiresAt().isAfter(now)) {
            throw new BookingException.HoldExpired("Your hold has expired, please start again");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setHoldExpiresAt(null);
        //PAYMENT
        return booking;
    }

    @Transactional
    public StorageBooking cancel(UUID orderId, UUID userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        StorageBooking booking = lockBooking(orderId);
        assertOwner(booking, userId);

        switch (booking.getStatus()) {
            case CANCELLED, EXPIRED -> { return booking; }
            case HELD -> { /* always cancellable */ }
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

    @Transactional
    public int expireStaleHolds() {
        int n = storageBookingRepository.expireHolds(BookingStatus.EXPIRED, BookingStatus.HELD, LocalDateTime.now(clock));
        if (n > 0) log.info("Expired {} stale booking holds", n);
        return n;
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
        storageRepository.existsByIsAvailableTrue();
    }

    private Storage lockStorage(UUID storageId) {
        try {
            return storageRepository.findByIdForUpdate(storageId)
                    .orElseThrow(() -> new BookingException.NotFound("Storage not found"));
        } catch (PessimisticLockingFailureException | LockTimeoutException e) {
            throw new BookingException.Busy("System is busy, please retry");
        }
    }

    private long billableMonths(LocalDateTime start, LocalDateTime end) {
        long months = ChronoUnit.MONTHS.between(start, end);
        if (start.plusMonths(months).isBefore(end)) {
            months++;
        }
        return Math.max(1, months);
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
