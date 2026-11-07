package com.warehouse.system.Utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class Computations {

    public BigDecimal billableMonths(LocalDateTime start, LocalDateTime end, BigDecimal priceCubic,double volumeCubic) {
        long months = ChronoUnit.MONTHS.between(start, end);
        if (start.plusMonths(months).isBefore(end)) {
            months++;
        }

        BigDecimal totalPrice = priceCubic
                .multiply(BigDecimal.valueOf(volumeCubic))
                .multiply(BigDecimal.valueOf(months))
                .setScale(2, RoundingMode.HALF_UP);

        return totalPrice;
    }

}
