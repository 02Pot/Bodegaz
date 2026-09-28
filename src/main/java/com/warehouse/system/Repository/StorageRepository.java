package com.warehouse.system.Repository;

import com.warehouse.system.Model.Storage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageRepository extends JpaRepository<Storage, UUID> {
    Optional<Storage> findByBlockName(String name);
    Optional<Storage> findBySection(String name);

    @Query("""
        SELECT u FROM Storage u 
        WHERE u.warehouse.warehouseId = :warehouseId 
        AND u.isAvailable = true 
        AND u.capacityKg >= :requiredCapacity
    """)
    Page<Storage> findAvailableUnitsInWarehouse(
            @Param("warehouseId") UUID warehouseId,
            Pageable pageable
    );

}
