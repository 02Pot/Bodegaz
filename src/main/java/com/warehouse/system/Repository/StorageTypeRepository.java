package com.warehouse.system.Repository;

import com.warehouse.system.Model.StorageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StorageTypeRepository extends JpaRepository<StorageType, UUID> {
}
