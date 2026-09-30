package com.warehouse.system.Controller;

import com.warehouse.system.DTO.Request.CreateHoldRequest;
import com.warehouse.system.DTO.Response.BookingResponse;
import com.warehouse.system.Model.StorageBooking;
import com.warehouse.system.Service.Booking.StorageBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final StorageBookingService service;
    private final Clock clock;

    @PostMapping("/create")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateHoldRequest request, Authentication auth) {
        StorageBooking booking = service.createBooking(currentUserId(auth), request);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/bookings/{id}").buildAndExpand(booking.getOrderId()).toUri();
        return ResponseEntity.created(location).body(toResponse(booking));
    }

    @PostMapping("/{orderId}/confirm")
    public BookingResponse confirm(@PathVariable UUID orderId, Authentication auth) {
        return toResponse(service.confirmBooking(orderId, currentUserId(auth)));
    }

    @PostMapping("/{orderId}/cancel")
    public BookingResponse cancel(@PathVariable UUID orderId, Authentication auth) {
        return toResponse(service.cancel(orderId, currentUserId(auth)));
    }

    private BookingResponse toResponse(StorageBooking b) {
        return BookingResponse.from(b, LocalDateTime.now(clock));
    }

    private UUID currentUserId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
