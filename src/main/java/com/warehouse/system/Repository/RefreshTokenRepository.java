package com.warehouse.system.Repository;

import com.warehouse.system.Model.RefreshTokenModel;
import com.warehouse.system.Model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenModel, Integer> {
    Optional<RefreshTokenModel> findByToken(String token);
    void deleteAllByUserAndRevokedFalse(UserModel user);

}
