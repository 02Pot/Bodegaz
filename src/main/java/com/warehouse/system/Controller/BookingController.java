package com.warehouse.system.Controller;

import com.warehouse.system.DTO.BookingRental.OwnerBookingStats;
import com.warehouse.system.DTO.Capacity;
import com.warehouse.system.DTO.OwnerBookingResponse;
import com.warehouse.system.DTO.Request.CreateHoldRequest;
import com.warehouse.system.DTO.Response.BookingDetailResponse;
import com.warehouse.system.DTO.Response.BookingResponse;
import com.warehouse.system.Enums.BookingStatus;
import com.warehouse.system.Model.StorageBooking;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Service.Booking.StorageBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final StorageBookingService service;
    private final Clock clock;

    @PostMapping("/create")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateHoldRequest request, @AuthenticationPrincipal UserModel user) {
        StorageBooking booking = service.createBooking(user.getId(), request);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/bookings/{id}").buildAndExpand(booking.getOrderId()).toUri();
        return ResponseEntity.created(location).body(toResponse(booking));
    }

    @PostMapping("/{orderId}/approve")
    public BookingResponse approve(@AuthenticationPrincipal UserModel user, @PathVariable UUID orderId) {
        return BookingResponse.from(service.approve(orderId, user.getId()),LocalDateTime.now());
    }

    @PostMapping("/{orderId}/reject")
    public BookingResponse reject(@AuthenticationPrincipal UserModel user, @PathVariable UUID orderId) {
        return BookingResponse.from(service.reject(orderId, user.getId()),LocalDateTime.now());
    }

    @GetMapping("/{storageId}")
    public List<BookingResponse> getStorageBookings(@PathVariable UUID storageId,@AuthenticationPrincipal UserModel user) {
        LocalDateTime now = LocalDateTime.now(clock);
        return service.viewStorageBookings(storageId, user.getId())
                .stream()
                .map(b -> BookingResponse.from(b, now))
                .toList();
    }

    @GetMapping("/owner")
    public Page<OwnerBookingResponse> ownerBookings(
            @AuthenticationPrincipal UserModel user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BookingStatus status) {
        return service.getBookingsByOwner(user.getId(), page, Math.min(Math.max(size, 1), 50), search, status);
    }

    @GetMapping("/owner/stats")
    public OwnerBookingStats ownerStats(@AuthenticationPrincipal UserModel user) {
        return service.getOwnerStats(user.getId());
    }

    @GetMapping("/my-bookings")
    public List<BookingDetailResponse> getMyBookings(@AuthenticationPrincipal UserModel user) {
        return service.getBookingsByUser(user.getId());
    }

    @PostMapping("/{orderId}/cancel")
    public BookingResponse cancel(@PathVariable UUID orderId, @AuthenticationPrincipal UserModel user) {
        return toResponse(service.cancel(orderId, user.getId()));
    }

    @GetMapping("/{storageId}/capacity")
    public BookingResponse.CapacityResponse capacity(
            @PathVariable UUID storageId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate){
        Capacity c = service.getRemainingCapacity(storageId, startDate, endDate);
        return new BookingResponse.CapacityResponse(c.availableVolume(),c.availableWeight());
    }

    private BookingResponse toResponse(StorageBooking b) {
        return BookingResponse.from(b, LocalDateTime.now(clock));
    }

}
