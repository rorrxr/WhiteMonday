package com.minju.user.controller;

import com.minju.common.dto.CommonResponse;
import com.minju.common.exception.BusinessException;
import com.minju.common.exception.ErrorCode;
import com.minju.user.entity.VerificationToken;
import com.minju.user.service.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationTokenService tokenService;

    /** 토큰 생성 */
    @PostMapping("/generate")
    public ResponseEntity<CommonResponse<String>> generateToken(@RequestParam String email) {
        if (email == null || email.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이메일은 필수 값입니다.");
        }

        VerificationToken token = tokenService.createVerificationToken(email);

        return ResponseEntity.ok(
                CommonResponse.success("인증 토큰이 생성되었습니다.", token.getToken())
        );
    }

    /** 토큰 검증 */
    @GetMapping("/validate")
    public ResponseEntity<CommonResponse<Void>> validateToken(@RequestParam String token) {
        Optional<VerificationToken> validToken = tokenService.validateToken(token);

        if (validToken.isPresent()) {
            tokenService.markTokenAsVerified(token);
            return ResponseEntity.ok(
                    CommonResponse.success("토큰 검증에 성공했습니다.", null)
            );
        } else {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "유효하지 않거나 만료된 토큰입니다.");
        }
    }
}
