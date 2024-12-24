package com.minju.whitemonday.controller;

import com.minju.whitemonday.dto.ResponseData;
import com.minju.whitemonday.dto.SignupRequestDto;
import com.minju.whitemonday.dto.UserInfoDto;
import com.minju.whitemonday.entity.UserRoleEnum;
import com.minju.whitemonday.entity.VerificationToken;
import com.minju.whitemonday.jwt.JwtUtil;
import com.minju.whitemonday.repository.UserRepository;
import com.minju.whitemonday.repository.VerificationTokenRepository;
import com.minju.whitemonday.security.UserDetailsImpl;
import com.minju.whitemonday.service.LogoutService;
import com.minju.whitemonday.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final LogoutService logoutService;
    private final JwtUtil jwtUtil;

    // 1. 회원가입
    @PostMapping("/signup")
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

    // 2. 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        try {
            String token = userService.login(username, password);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7); // "Bearer " 제거
        }

        log.info("Logging out token: {}", token);

        if (jwtUtil.validateToken(token)) {
            logoutService.invalidateToken(token); // 블랙리스트 추가
            log.info("Token invalidated successfully: {}", token);
            return ResponseEntity.ok("Logged out successfully");
        } else {
            log.error("Invalid token: {}", token);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }

    // 4. 사용자 정보 조회
    @GetMapping("/info")
    public ResponseEntity<UserInfoDto> getUserInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        String username = userDetails.getUsername();
        UserRoleEnum role = userDetails.getUser().getRole();
        boolean isAdmin = (role == UserRoleEnum.ADMIN);

        UserInfoDto userInfoDto = new UserInfoDto(username, isAdmin);
        return ResponseEntity.ok(userInfoDto);
    }

    // 5. 비밀번호 변경
//    @PostMapping("/update-password")
//    public ResponseEntity<String> updatePassword(@RequestBody Map<String, String> passwordUpdateRequest) {
//        String username = passwordUpdateRequest.get("username");
//        String newPassword = passwordUpdateRequest.get("newPassword");
//
//        userService.updatePassword(username, newPassword);
//        return ResponseEntity.ok("Password updated successfully");
//    }
}

