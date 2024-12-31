package com.minju.user.repository;

import com.minju.user.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByEmail(String email);

    void deleteByExpiresAtBefore(LocalDateTime now);
}
