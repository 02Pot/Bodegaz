package com.warehouse.system.Repository;

import com.warehouse.system.Model.OtpModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<OtpModel, UUID> {
    Optional<OtpModel> findTopByOtpAndUsedFalseOrderByCreatedAtDesc(String otp);
    Optional<OtpModel> findTopByEmailOrderByCreatedAtDesc(String userEmail);
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpModel o WHERE o.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);

    @Modifying
    @Transactional
    void deleteAllByEmail(String email);
}
