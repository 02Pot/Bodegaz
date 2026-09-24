package com.warehouse.system.Repository;

import com.warehouse.system.Model.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserModelRepository extends JpaRepository<UserModel, UUID> {
    Optional<UserModel> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<UserModel> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );
    Page<UserModel> findByEmailContainingIgnoreCase(
            String email,
            Pageable pageable
    );
}
