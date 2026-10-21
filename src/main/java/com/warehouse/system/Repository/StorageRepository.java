package com.warehouse.system.Repository;

import com.warehouse.system.DTO.Interface.StorageStats;
import com.warehouse.system.Model.Storage;
import com.warehouse.system.Model.Warehouse;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageRepository extends JpaRepository<Storage, UUID> {
    Optional<Storage> findByBlockName(String name);
    Optional<Storage> findBySection(String name);
    List<Storage> findByWarehouse_WarehouseId(UUID warehouseId);
    @Query("""
        select s.warehouse.warehouseId as warehouseId,
               count(s) as storageCount,
               coalesce(sum(s.maxWeight), 0) as allocatedKg
        from Storage s
        where s.warehouse.user.id = :userId
        group by s.warehouse.warehouseId
    """)
    List<StorageStats> statsByOwner(@Param("userId") UUID userId);

    boolean existsByIsAvailableTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select s from Storage s where s.id = :id")
    Optional<Storage> findByIdForUpdate(@Param("id") UUID id);

    @Query("select coalesce(sum(s.maxWeight), 0.0) from Storage s " +
            "where s.warehouse = :warehouse and s.storageId <> :storageId")
    Double sumMaxWeightOfOthers(@Param("warehouse") Warehouse warehouse,
                                @Param("storageId") UUID storageId);
}
