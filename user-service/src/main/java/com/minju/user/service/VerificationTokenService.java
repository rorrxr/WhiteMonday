package com.minju.user.service;

import com.minju.user.entity.VerificationToken;
import com.minju.user.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {
    private final VerificationTokenRepository tokenRepository;

    public VerificationToken createVerificationToken(String email) {
        VerificationToken token = new VerificationToken(email);
        return tokenRepository.save(token);
    }

    public Optional<VerificationToken> validateToken(String token) {
        return tokenRepository.findByToken(token)
                .filter(t -> !t.isExpired())
                .filter(t -> !t.isVerified());
    }

    public void markTokenAsVerified(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        verificationToken.markAsVerified();
        tokenRepository.save(verificationToken);
    }

    public void cleanExpiredTokens() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}