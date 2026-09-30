package com.warehouse.system.Utils;

import com.warehouse.system.Service.Booking.StorageBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingHoldJob {
    private final StorageBookingService service;

    @Scheduled(fixedDelayString = "${booking.hold-sweep-ms:60000}")
    public void sweep(){
        service.expireStaleHolds();
    }
}
