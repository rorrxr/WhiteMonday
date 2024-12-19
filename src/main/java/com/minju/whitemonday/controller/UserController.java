package com.minju.whitemonday.controller;

import com.minju.whitemonday.dto.SignupRequestDto;
import com.minju.whitemonday.dto.UserInfoDto;
import com.minju.whitemonday.entity.UserRoleEnum;
import com.minju.whitemonday.entity.VerificationToken;
import com.minju.whitemonday.repository.UserRepository;
import com.minju.whitemonday.repository.VerificationTokenRepository;
import com.minju.whitemonday.security.UserDetailsImpl;
import com.minju.whitemonday.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;

    @GetMapping("/user-info")
    public ResponseEntity<UserInfoDto> join(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        // UserDetailsImpl에서 사용자 정보 가져오기
        String username = userDetails.getUsername();
        UserRoleEnum role = userDetails.getUser().getRole();
        boolean isAdmin = (role == UserRoleEnum.ADMIN);

        // UserInfoDto 객체 생성
        UserInfoDto userInfoDto = new UserInfoDto(username, isAdmin);

        // ResponseEntity로 반환
        return ResponseEntity.ok(userInfoDto);
    }

    // 로그인 데이터를 받아 인증을 수행합니다.
    @PostMapping("/api/user/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequestDto requestDto, BindingResult bindingResult) {
        // Validation 예외 처리
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError fieldError : bindingResult.getFieldErrors()) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(errors);
        }

        userService.signup(requestDto);
        return ResponseEntity.ok("Signup successful");
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
