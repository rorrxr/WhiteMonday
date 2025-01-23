package com.minju.user.controller;

import com.minju.user.entity.User;
import com.minju.user.repository.UserRepository;
import com.minju.user.service.UserService;
import com.minju.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

//@RestController
//@RequestMapping("/api/auth")
//@RequiredArgsConstructor
//@Slf4j
//public class AuthenticationController {
//    private final JwtUtil jwtUtil;
//    private final UserRepository userRepository;
//    private final UserService userService;
//
//    @PostMapping("/refresh")
//    public ResponseEntity<?> refreshAccessToken(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
//        if (authorizationHeader == null || !authorizationHeader.startsWith(JwtUtil.BEARER_PREFIX)) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing Authorization header");
//        }
//
//        String refreshToken = authorizationHeader.substring(JwtUtil.BEARER_PREFIX.length());
//        log.info("Received refresh token: {}", refreshToken);
//
//        try {
//            String newAccessToken = userService.refreshAccessToken(refreshToken);
//            log.info("Generated new Access Token: {}", newAccessToken);
//            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
//        } catch (IllegalArgumentException e) {
//            log.error("Error during token refresh: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
//        }
//    }
//}
