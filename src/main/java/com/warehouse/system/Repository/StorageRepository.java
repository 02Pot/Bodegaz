package com.warehouse.system.Repository;

import com.warehouse.system.Model.Storage;
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
    boolean existsByIsAvailableTrue();

    @Query("""
        SELECT s FROM Storage s
        WHERE s.warehouse.warehouseId = :warehouseId
        AND s.isAvailable = true
    """)
    Page<Storage> findAvailableUnitsInWarehouse(
            @Param("warehouseId") UUID warehouseId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select s from Storage s where s.id = :id")
    Optional<Storage> findByIdForUpdate(@Param("id") UUID id);
}
