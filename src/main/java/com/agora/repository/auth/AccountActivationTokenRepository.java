package com.agora.repository.auth;

import com.agora.entity.auth.AccountActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, UUID> {

    void deleteByUser_Id(UUID userId);

    Optional<AccountActivationToken> findByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AccountActivationToken t set t.usedAt = :now where t.user.id = :userId and t.usedAt is null")
    void markAllUnusedAsRevoked(@Param("userId") UUID userId, @Param("now") Instant now);
}
