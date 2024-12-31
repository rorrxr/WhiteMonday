package com.minju.user.controller;

import com.minju.user.entity.User;
import com.minju.user.repository.UserRepository;
import com.minju.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestHeader("Authorization") String refreshToken) {
        if (!refreshToken.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid refresh token format"));
        }

        String token = refreshToken.substring(7);
        if (!jwtUtil.validateRefreshToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired refresh token"));
        }

        String username = jwtUtil.getUserInfoFromToken(token).getSubject();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String newAccessToken = jwtUtil.createToken(user.getUsername(), user.getRole());
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }
}
