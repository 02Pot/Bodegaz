package com.warehouse.system.Repository;

import com.warehouse.system.Model.SavedWarehouses;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface SavedWarehouseRepository extends JpaRepository<SavedWarehouses,UUID> {
    boolean existsByUser_IdAndWarehouse_WarehouseId(UUID userId, UUID warehouseId);
    Set<UUID> findByUserIdAndWarehouse_WarehouseIdIn(UUID userId, Collection<UUID> ids);

    @Modifying
    void deleteByUserIdAndWarehouse_WarehouseId(UUID userId, UUID warehouseId);

    @EntityGraph(attributePaths = {"warehouse", "warehouse.warehouseAddress", "warehouse.user"})
    Slice<SavedWarehouses> findByUser_IdAndIdGreaterThanOrderByIdAsc(UUID userId, UUID cursor, Pageable pageable);
}
