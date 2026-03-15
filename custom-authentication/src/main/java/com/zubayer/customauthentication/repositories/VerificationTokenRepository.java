package com.zubayer.customauthentication.repositories;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

// Local Imports
import com.zubayer.customauthentication.models.UserEntity;
import com.zubayer.customauthentication.enums.VerificationTokenStatus;
import com.zubayer.customauthentication.models.VerificationTokenEntity;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationTokenEntity, Long> {

    Optional<VerificationTokenEntity> findByToken(String token);

    @Query("SELECT v FROM VerificationTokenEntity v WHERE v.userEntity = :user AND v.verificationTokenStatus = :status")
    Optional<VerificationTokenEntity> findByUserEntity_IdAndVerificationTokenStatus(@Param("user") UserEntity user, @Param("status") VerificationTokenStatus status);
}
