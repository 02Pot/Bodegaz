package com.warehouse.system.Repository;

import com.warehouse.system.Model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {
    Optional<Warehouse> findByName(String warehouseName);
    Optional<Warehouse> findByUser_Id(UUID userId);

}
