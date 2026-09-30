package com.warehouse.system.Repository;

import com.warehouse.system.Enums.BookingStatus;
import com.warehouse.system.Model.StorageBooking;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface StorageBookingRepository extends JpaRepository<StorageBooking, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select b from StorageBooking b where b.orderId = :id")
    Optional<StorageBooking> findByIdForUpdate(@Param("id") UUID id);
    boolean existsByUnitNumberAndOrderIdNot(String unitNumber, UUID orderId);

    @Query("""
        select count(b) from StorageBooking b
        where b.storage.id = :storageId
          and b.unitNumber = :unitNumber
          and b.startDate < :end
          and b.endDate > :start
          and (b.status = :confirmed or (b.status = :held and b.holdExpiresAt > :now))
        """)
    long countBlocking(@Param("storageId") UUID storageId,
                       @Param("unitNumber") String unitNumber,
                       @Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end,
                       @Param("confirmed") BookingStatus confirmed,
                       @Param("held") BookingStatus held,
                       @Param("now") LocalDateTime now);

    default boolean isUnitBlocked(UUID storageId, String unit, LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        return countBlocking(storageId, unit, start, end, BookingStatus.CONFIRMED, BookingStatus.HELD, now) > 0;
    }

    @Query("""
        select count(b) from StorageBooking b
        where b.user.id = :userId and b.status = :held and b.holdExpiresAt > :now
        """)
    long countActiveHolds(@Param("userId") UUID userId,
                          @Param("held") BookingStatus held,
                          @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update StorageBooking b set b.status = :expired
        where b.status = :held and b.holdExpiresAt <= :now
        """)
    int expireHolds(@Param("expired") BookingStatus expired, @Param("held") BookingStatus held, @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update StorageBooking b set b.status = :expired
        where b.storage.id = :storageId and b.status = :held and b.holdExpiresAt <= :now
        """)
    int expireHoldsForStorage(@Param("storageId") UUID storageId,
                              @Param("expired") BookingStatus expired,
                              @Param("held") BookingStatus held,
                              @Param("now") LocalDateTime now);

}
