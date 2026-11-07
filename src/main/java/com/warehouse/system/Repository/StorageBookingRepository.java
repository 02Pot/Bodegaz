package com.warehouse.system.Repository;

import com.warehouse.system.DTO.Interface.BookedStats;
import com.warehouse.system.DTO.Response.StorageUsage;
import com.warehouse.system.Enums.BookingStatus;
import com.warehouse.system.Model.StorageBooking;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageBookingRepository extends JpaRepository<StorageBooking, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select b from StorageBooking b where b.orderId = :id")
    Optional<StorageBooking> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
    select b from StorageBooking b
    join fetch b.user
    join fetch b.storage s
    join fetch s.warehouse w
    where w.user.id = :ownerId
      and b.status not in :hidden
    order by b.startDate desc
    """)
    List<StorageBooking> findByOwner(@Param("ownerId") UUID ownerId,
                                     @Param("hidden") Collection<BookingStatus> hidden);

    @Query("""
        SELECT b FROM StorageBooking b
        JOIN FETCH b.storage s
        JOIN FETCH s.warehouse w
        JOIN FETCH w.warehouseAddress
        WHERE b.user.id = :userId
    """)
    List<StorageBooking> findByUser_IdWithDetails(UUID userId);

    @Query("""
        select b from StorageBooking b
        JOIN FETCH b.user
        WHERE b.storage.storageId = :storageId
        ORDER by b.startDate desc
        """)
    List<StorageBooking> findAllByStorageId(@Param("storageId") UUID storageId);

    @Query("select new com.warehouse.system.DTO.Response.StorageUsage(" +
            "coalesce(sum(b.rentedVolumeCubicMeter), 0.0), " +
            "coalesce(sum(b.rentedWeightKg), 0.0)) " +
            "from StorageBooking b " +
            "where b.storage.storageId = :storageId " +
            "and b.startDate < :end " +
            "and b.endDate > :start " +
            "and (b.status = :confirmed " +
            "or (b.status = :held and b.holdExpiresAt > :now))")
    StorageUsage sumOverlappingUsage(
            @Param("storageId") UUID storageId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("now") LocalDateTime now,
            @Param("confirmed") BookingStatus confirmed,
            @Param("held") BookingStatus held);

    @Query("select count(b) from StorageBooking b " +
            "where b.user.id = :userId " +
            "and b.status = :held " +
            "and b.holdExpiresAt > :now")
    long countActiveHolds(
            @Param("userId") UUID userId,
            @Param("held") BookingStatus held,
            @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true)
    @Query("update StorageBooking b " +
            "set b.status = :expired, b.holdExpiresAt = null " +
            "where b.status = :held " +
            "and b.holdExpiresAt <= :now")
    int expireHolds(
            @Param("expired") BookingStatus expired,
            @Param("held") BookingStatus held,
            @Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true)
    @Query("update StorageBooking b " +
            "set b.status = :expired, b.holdExpiresAt = null " +
            "where b.storage.storageId = :storageId " +
            "and b.status = :held " +
            "and b.holdExpiresAt <= :now")
    int expireHoldsForStorage(
            @Param("storageId") UUID storageId,
            @Param("expired") BookingStatus expired,
            @Param("held") BookingStatus held,
            @Param("now") LocalDateTime now);

    @Query("""
        select s.warehouse.warehouseId as warehouseId,
               count(distinct s.storageId) as bookedCount
        from StorageBooking b join b.storage s
        where s.warehouse.user.id = :userId
          and b.status in :statuses
        group by s.warehouse.warehouseId
    """)
    List<BookedStats> bookedByOwner(@Param("userId") UUID userId,
                                    @Param("statuses") Collection<BookingStatus> statuses);
}
