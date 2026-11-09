package com.warehouse.system.Specs;

import com.warehouse.system.DTO.Warehouse.WarehouseFilter;
import com.warehouse.system.Model.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class WarehouseSpecs {

    public static Specification<Warehouse> feed(UUID userId, WarehouseFilter f, Warehouse cursor, boolean asc,boolean savedOnly) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();

            Subquery<UUID> saved = q.subquery(UUID.class);
            Root<SavedWarehouses> sw = saved.from(SavedWarehouses.class);
            saved.select(sw.get("id")).where(
                    cb.equal(sw.get("warehouse"), root),
                    cb.equal(sw.get("user").get("id"), userId));
            Predicate isSaved = cb.exists(saved);
            p.add(savedOnly ? isSaved : cb.not(isSaved));

            // search + city
            Join<Warehouse, WarehouseAddress> addr = root.join("warehouseAddress");
            if (hasText(f.search())) {
                String like = "%" + f.search().trim().toLowerCase() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(addr.get("city")), like),
                        cb.like(cb.lower(addr.get("addressLine1")), like)));
            }
            if (hasText(f.city()))
                p.add(cb.equal(cb.lower(addr.get("city")), f.city().trim().toLowerCase()));

            if (f.minCapacity() != null)
                p.add(cb.ge(root.get("warehouseCapacityKg"), f.minCapacity()));

            if (f.customDate() != null)
                p.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"),
                        f.customDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));

            // storage-level filters: has at least one listed unit matching
            if (f.minBudget() != null || f.maxBudget() != null || f.minArea() != null || f.maxArea() != null) {
                Subquery<UUID> sq = q.subquery(UUID.class);
                Root<Storage> s = sq.from(Storage.class);
                List<Predicate> sp = new ArrayList<>();
                sp.add(cb.equal(s.get("warehouse"), root));
                sp.add(cb.isTrue(s.get("listed")));
                if (f.minBudget() != null) sp.add(cb.ge(s.<BigDecimal>get("pricePerCubicMeter"), f.minBudget()));
                if (f.maxBudget() != null) sp.add(cb.le(s.<BigDecimal>get("pricePerCubicMeter"), f.maxBudget()));
                if (f.minArea() != null || f.maxArea() != null) {
                    Path<StorageType> t = s.get("storageType");
                    Expression<Double> area = cb.prod(t.<Double>get("lengthMeters"), t.<Double>get("breadthMeters"));
                    if (f.minArea() != null) sp.add(cb.ge(area, f.minArea()));
                    if (f.maxArea() != null) sp.add(cb.le(area, f.maxArea()));
                }
                sq.select(s.get("storageId")).where(sp.toArray(Predicate[]::new));
                p.add(cb.exists(sq));
            }

            // keyset cursor
            if (cursor != null) {
                Path<Instant> ca = root.get("createdAt");
                Path<UUID> id = root.get("warehouseId");
                p.add(asc
                        ? cb.or(cb.greaterThan(ca, cursor.getCreatedAt()),
                        cb.and(cb.equal(ca, cursor.getCreatedAt()), cb.greaterThan(id, cursor.getWarehouseId())))
                        : cb.or(cb.lessThan(ca, cursor.getCreatedAt()),
                        cb.and(cb.equal(ca, cursor.getCreatedAt()), cb.lessThan(id, cursor.getWarehouseId()))));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String s) { return s != null && !s.isBlank(); }
}

