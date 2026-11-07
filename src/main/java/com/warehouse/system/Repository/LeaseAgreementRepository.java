package com.warehouse.system.Repository;

import com.warehouse.system.Model.LeaseAgreement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaseAgreementRepository extends JpaRepository<LeaseAgreement, UUID> {
    Optional<LeaseAgreement> findByAgreementNumber(String number);
    Optional<LeaseAgreement> findByStorageBooking_OrderId(UUID orderId);

    @EntityGraph(attributePaths = "storageBooking")
    List<LeaseAgreement> findByStorageBooking_User_IdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = "storageBooking")
    List<LeaseAgreement> findByStorageBooking_Storage_Warehouse_User_IdOrderByCreatedAtDesc(UUID ownerUserId);
}
