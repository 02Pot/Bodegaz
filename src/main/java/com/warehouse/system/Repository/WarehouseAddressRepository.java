package com.warehouse.system.Repository;

import com.warehouse.system.Model.WarehouseAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WarehouseAddressRepository extends JpaRepository<WarehouseAddress, UUID> {
}
