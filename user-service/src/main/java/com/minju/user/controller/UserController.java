package com.minju.user.controller;

import com.minju.user.dto.SignupRequestDto;
import com.minju.user.dto.UserInfoDto;
import com.minju.user.dto.UserRoleEnum;
import com.minju.user.entity.User;
import com.minju.user.repository.UserRepository;
import com.minju.user.service.LogoutService;
import com.minju.user.service.UserDetailsImpl;
import com.minju.user.service.UserService;
import com.minju.user.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private Environment env;

    private final UserService userService;
    private final UserRepository userRepository;
    private final LogoutService logoutService;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserController(Environment env, UserService userService, UserRepository userRepository, LogoutService logoutService, JwtUtil jwtUtil) {
        this.env = env;
        this.userService = userService;
        this.userRepository = userRepository;
        this.logoutService = logoutService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/test")
    public String status() {
        return String.format("It's Working in User Service on PORT %s", env.getProperty("local.server.port"));
    }

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
//    @PostMapping("/login")
//    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
//        String username = loginRequest.get("username");
//        String password = loginRequest.get("password");
//
//        try {
//            String token = userService.login(username, password);
//            return ResponseEntity.ok(Map.of("token", token));
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
//        }
//    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        try {
            Map<String, String> tokens = userService.login(username, password);
            return ResponseEntity.ok(tokens);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

//    @PostMapping("/logout")
//    public ResponseEntity<String> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
//        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
//            log.error("Authorization header is missing or invalid: {}", authorizationHeader);
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing Authorization header");
//        }
//
//        String token = authorizationHeader.substring(7); // "Bearer " 제거
//        log.info("Logging out token: {}", token);
//
//        try {
//            // 토큰 검증
//            if (jwtUtil.validateToken(token)) {
//                logoutService.invalidateToken(token); // 블랙리스트에 추가
//                log.info("Token invalidated successfully: {}", token);
//
//                // SecurityContext 초기화
//                SecurityContextHolder.clearContext();
//
//                return ResponseEntity.ok("Logged out successfully");
//            } else {
//                log.warn("Token validation failed");
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
//            }
//        } catch (ExpiredJwtException e) {
//            log.warn("Token expired but adding to blacklist for safety");
//            logoutService.invalidateToken(token); // 만료된 토큰도 블랙리스트에 추가
//
//            // SecurityContext 초기화
//            SecurityContextHolder.clearContext();
//
//            return ResponseEntity.ok("Logged out successfully");
//        } catch (Exception e) {
//            log.error("Unexpected error during logout: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Logout failed");
//        }
//    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(JwtUtil.BEARER_PREFIX)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing Authorization header");
        }

        String refreshToken = authorizationHeader.substring(JwtUtil.BEARER_PREFIX.length());

        try {
            userService.logout(refreshToken);
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok("Logged out successfully");
        } catch (Exception e) {
            log.error("Unexpected error during logout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Logout failed");
        }
    }


    // 4. 사용자 정보 조회
//    @GetMapping("/info")
//    public ResponseEntity<UserInfoDto> getUserInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
//        String username = userDetails.getUsername();
//        UserRoleEnum role = userDetails.getUser().getRole();
//        boolean isAdmin = (role == UserRoleEnum.ADMIN);
//
//        UserInfoDto userInfoDto = new UserInfoDto(username, isAdmin);
//        return ResponseEntity.ok(userInfoDto);
//    }

    @GetMapping("/info")
    public ResponseEntity<UserInfoDto> getUserInfo(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        log.info("Fetching user info for userId: {}", userId);
        UserInfoDto userInfo = userService.getUserInfo(userId);
        return ResponseEntity.ok(userInfo);
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

