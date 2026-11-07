package com.warehouse.system.Repository;

import com.warehouse.system.Model.WarehouseImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WarehouseImageRepository extends JpaRepository<WarehouseImage, UUID> {
    List<WarehouseImage> findByWarehouse_WarehouseId(UUID warehouseId);
    List<WarehouseImage> findByWarehouse_WarehouseIdInOrderByUploadedAtAsc(Collection<UUID> warehouseIds);
    List<WarehouseImage> findByIdInAndWarehouse_WarehouseId(Collection<UUID> ids, UUID warehouseId);
}
