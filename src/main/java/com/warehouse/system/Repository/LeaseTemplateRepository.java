package com.warehouse.system.Repository;

import com.warehouse.system.Model.LeaseTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaseTemplateRepository extends JpaRepository<LeaseTemplate, UUID> {

    Optional<LeaseTemplate> findFirstByPlatformDefaultTrueAndActiveTrue();

    List<LeaseTemplate> findByOwnerIdAndActiveTrueOrderByCreatedAtDesc(UUID ownerId);

    Optional<LeaseTemplate> findByIdAndActiveTrue(UUID id);
}
