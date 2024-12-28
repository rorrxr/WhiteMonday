package com.minju.user.controller;

import com.minju.user.entity.VerificationToken;
import com.minju.user.repository.UserRepository;
import com.minju.user.repository.VerificationTokenRepository;
import com.minju.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class EmailController {

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @PostMapping("/send-verification-email")
    public ResponseEntity<Map<String, String>> sendVerificationEmail(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        Map<String, String> response = new HashMap<>();

        if (email == null || email.isEmpty()) {
            response.put("error", "이메일은 필수 항목입니다. Email is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        VerificationToken token = new VerificationToken(email);
        tokenRepository.save(token);
        emailService.sendVerificationToken(email, token.getToken());

        response.put("message", "인증 이메일이 성공적으로 전송되었습니다. Verification email sent successfully.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestBody Map<String, String> requestBody) {
        String token = requestBody.get("token");
        Map<String, String> response = new HashMap<>();

        if (token == null || token.isEmpty()) {
            response.put("error", "토큰은 필수 항목입니다. Token is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    response.put("error", "유효하지 않은 토큰입니다. Invalid token");
                    return new IllegalArgumentException("유효하지 않은 토큰입니다. Invalid token");
                });

        if (verificationToken.isVerified()) {
            response.put("message", "토큰이 이미 인증되었습니다. Token has already been verified.");
            return ResponseEntity.ok(response);
        }

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            response.put("error", "토큰이 만료되었습니다. Token has expired");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        verificationToken.setVerified(true);
        tokenRepository.save(verificationToken);

        userRepository.findByEmail(verificationToken.getEmail()).ifPresent(user -> {
            user.setEnabled(true);
            userRepository.save(user);
        });

        response.put("message", "이메일 인증이 완료되었습니다! Email verification successful!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/verify-email")
    public String verifyEmail(@RequestParam("token") String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("토큰이 만료되었습니다.");
        }

        verificationToken.setVerified(true);
        tokenRepository.save(verificationToken);

        // 이메일을 기준으로 사용자를 활성화
        userRepository.findByEmail(verificationToken.getEmail()).ifPresent(user -> {
            user.setEnabled(true); // 활성화 상태로 변경
            userRepository.save(user);
        });

        return "이메일 인증이 완료되었습니다!";
    }
}