package com.minju.user.service;

import com.minju.user.dto.SignupRequestDto;
import com.minju.user.dto.UserInfoDto;
import com.minju.user.dto.UserRoleEnum;
import com.minju.user.entity.User;
import com.minju.user.repository.UserRepository;
import com.minju.user.util.JwtUtil;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LogoutService logoutService;

    @Retry(name = "database-operation", fallbackMethod = "registerUserFallback")
    public User signup(SignupRequestDto requestDto) {
        log.info("Starting signup for user: {}", requestDto.getUsername());

        // 중복 확인
        if (userRepository.findByUsername(requestDto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 사용자입니다.");
        }

        if (userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 사용자 저장
//        User user = User.builder()
//                .username(requestDto.getUsername())
//                .password(passwordEncoder.encode(requestDto.getPassword()))
//                .name(encryptionUtil.encrypt(requestDto.getName()))
//                .build();

        User user = User.builder()
        .username(requestDto.getUsername())
        .password(passwordEncoder.encode(requestDto.getPassword()))
        .email(requestDto.getEmail())
        .role(UserRoleEnum.USER)
        .isEnabled(true)
        .build();

//        User savedUser = userRepository.save(user);
        // 이메일 발송
//        emailService.sendVerificationEmail(savedUser.getEmail());
        //return UserInfoDto.from(savedUser);

        log.info("User signup successful for: {}", requestDto.getUsername());

        return userRepository.save(user);
    }

    // 로그인
    public Map<String, String> login(String username, String password) {
        log.info("Attempting login for username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtUtil.createAccessToken(user.getId(), username, user.getRole().getAuthority());

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        return tokens;
    }

    // 로그아웃
    public void logout(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        String username = jwtUtil.extractUsername(token);
        log.info("Logging out user with username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        logoutService.invalidateToken(token); // 토큰 블랙리스트 추가
        log.info("Token invalidated successfully for user: {}", username);
    }

    // Access Token 재발급
//    public String refreshAccessToken(String accessToken) {
//        if (!jwtUtil.validateToken(accessToken)) {
//            throw new IllegalArgumentException("액세스 토큰이 유효하지 않습니다.");
//        }
//        String username = jwtUtil.extractClaims(accessToken).getSubject();
//        userRepository.findByUsername(username)
//                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
//
//        return jwtUtil.createAccessToken(username, "ROLE_USER");
//    }

    public UserInfoDto getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserInfoDto(user.getUsername(), user.getRole().equals(UserRoleEnum.ADMIN));
    }

    // fallback 함수 추가
//    public UserInfoDto registerUserFallback(SignupRequestDto request, Exception ex) {
//        log.error("사용자 등록 실패: {}", ex.getMessage());
//        throw new UserRegistrationException("사용자 등록 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
//    }
}