package com.ibetcha.identity.infrastructure;

import com.ibetcha.identity.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByJti(String jti);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = CURRENT_TIMESTAMP " +
           "WHERE rt.tokenFamily = :family AND rt.revokedAt IS NULL")
    void revokeFamily(@Param("family") String family);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = CURRENT_TIMESTAMP " +
           "WHERE rt.userId = :userId AND rt.revokedAt IS NULL")
    void revokeAllForUser(@Param("userId") UUID userId);
}
