package com.minju.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LogoutService {
    private final ConcurrentHashMap<String, Long> tokenBlacklist = new ConcurrentHashMap<>();

    // 토큰 블랙리스트 추가
    public void invalidateToken(String token) {
        tokenBlacklist.put(token, System.currentTimeMillis());
        log.info("Token invalidated: {}", token);
    }
    // 리프레시 토큰을 블랙리스트에 추가
//    public void invalidateRefreshToken(String token) {
//        refreshTokenBlacklist.put(token, System.currentTimeMillis());
//    }
    // 블랙리스트 확인
    public boolean isTokenBlacklisted(String token) {
        boolean isBlacklisted = tokenBlacklist.containsKey(token);
        log.info("Is token blacklisted? {}", isBlacklisted);
        return isBlacklisted;
    }
}

