package com.warehouse.system.Repository;

import com.warehouse.system.Model.Storage;
import com.warehouse.system.Model.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {
    Slice<Warehouse> findByWarehouseIdGreaterThanOrderByWarehouseIdAsc(UUID id, Pageable pageable);

    @Query("""
    select w
    from Warehouse w
    where w.warehouseId > :cursor
      and not exists (
        select 1
        from SavedWarehouses sw
        where sw.warehouse = w
          and sw.user.id = :userId
      )
    order by w.warehouseId asc
    """)
    Slice<Warehouse> findUnsavedFeed(
            @Param("cursor") UUID cursor,
            @Param("userId") UUID userId,
            Pageable pageable);
    @Query("""
      select i from Warehouse i
      where i.createdAt > :since
        order by i.viewCount desc, i.createdAt desc
    """)
    Page<Warehouse> findTrending(@Param("since") Instant since, Pageable pageable);
    @Query("""
      select i from Warehouse i
      order by i.createdAt desc
    """)
    Page<Warehouse> findNewest(Pageable pageable);
    @Query("""
    SELECT w FROM Warehouse w 
    WHERE w.warehouseCapacityKg >= :requiredCapacity
    """)
    Page<Warehouse> findByWarehouseCapacityKg(@Param("requiredCapacity") double requiredCapacity, Pageable pageable);
}
