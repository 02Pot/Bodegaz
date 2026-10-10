package com.warehouse.system.Repository;

import com.warehouse.system.Model.SavedWarehouses;
import com.warehouse.system.Model.Warehouse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.UUID;

public interface SavedWarehouseRepository extends JpaRepository<SavedWarehouses,UUID> {
    boolean existsByUser_IdAndWarehouse_WarehouseId(UUID userId, UUID warehouseId);

    @Query("""
        select w from SavedWarehouses s
        join s.warehouse w
        where s.user.id = :userId
          and w.warehouseId > :lastId
        order by w.warehouseId asc
    """)
        Slice<Warehouse> findSavedByUser(@Param("userId") UUID userId,
                                         @Param("lastId") UUID lastId,
                                         Pageable pageable);
    void deleteByUserIdAndWarehouse_WarehouseId(UUID userId, UUID warehouseId);

}
