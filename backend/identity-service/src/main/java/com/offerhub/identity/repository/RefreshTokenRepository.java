package com.offerhub.identity.repository;

import com.offerhub.identity.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Theft protection: a reused (already-revoked) refresh token means the user's session
     * chain has forked, so every other token still active for them gets pulled too, not
     * just the one that got reused.
     */
    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.userId = :userId and t.revoked = false")
    void revokeAllForUser(@Param("userId") UUID userId);
}
