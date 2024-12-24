package com.minju.whitemonday.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LogoutService {
    private final ConcurrentHashMap<String, Long> tokenBlacklist = new ConcurrentHashMap<>();

    public void invalidateToken(String token) {
        tokenBlacklist.put(token, System.currentTimeMillis());
        log.info("Token invalidated: {}", token);
    }

    public boolean isTokenBlacklisted(String token) {
        boolean isBlacklisted = tokenBlacklist.containsKey(token);
        log.info("Is token blacklisted? {}", isBlacklisted);
        return isBlacklisted;
    }
}

