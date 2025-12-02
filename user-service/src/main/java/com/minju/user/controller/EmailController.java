package com.minju.user.controller;

import com.minju.common.dto.CommonResponse;
import com.minju.common.exception.BusinessException;
import com.minju.common.exception.ErrorCode;
import com.minju.user.entity.VerificationToken;
import com.minju.user.repository.UserRepository;
import com.minju.user.repository.VerificationTokenRepository;
import com.minju.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class EmailController {

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /** 이메일 인증 메일 전송 */
    @PostMapping("/send-verification-email")
    public ResponseEntity<CommonResponse<Void>> sendVerificationEmail(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");

        if (email == null || email.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이메일은 필수 항목입니다.");
        }

        VerificationToken token = new VerificationToken(email);
        tokenRepository.save(token);
        emailService.sendVerificationToken(email, token.getToken());

        return ResponseEntity.ok(
                CommonResponse.success("인증 이메일이 성공적으로 전송되었습니다.", null)
        );
    }

    /** 토큰 기반 이메일 인증 (JSON 요청) */
    @PostMapping("/verify-email")
    public ResponseEntity<CommonResponse<Void>> verifyEmail(@RequestBody Map<String, String> requestBody) {
        String token = requestBody.get("token");

        if (token == null || token.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "토큰은 필수 항목입니다.");
        }

        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "유효하지 않은 토큰입니다."));

        if (verificationToken.isVerified()) {
            return ResponseEntity.ok(
                    CommonResponse.success("토큰이 이미 인증되었습니다.", null)
            );
        }

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.FAILED_TIME_PAYMENT, "토큰이 만료되었습니다."); // 전용 코드 있으면 그걸 사용
        }

        verificationToken.setVerified(true);
        tokenRepository.save(verificationToken);

        userRepository.findByEmail(verificationToken.getEmail()).ifPresent(user -> {
            user.setEnabled(true);
            userRepository.save(user);
        });

        return ResponseEntity.ok(
                CommonResponse.success("이메일 인증이 완료되었습니다!", null)
        );
    }

    /** 이메일 링크 클릭용 (브라우저 접근) */
    @GetMapping("/verify-email")
    public ResponseEntity<CommonResponse<Void>> verifyEmail(@RequestParam("token") String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "유효하지 않은 토큰입니다."));

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "토큰이 만료되었습니다.");
        }

        verificationToken.setVerified(true);
        tokenRepository.save(verificationToken);

        userRepository.findByEmail(verificationToken.getEmail()).ifPresent(user -> {
            user.setEnabled(true);
            userRepository.save(user);
        });

        return ResponseEntity.ok(
                CommonResponse.success("이메일 인증이 완료되었습니다!", null)
        );
    }
}
