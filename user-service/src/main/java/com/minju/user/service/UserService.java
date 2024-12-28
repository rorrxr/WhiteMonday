package com.minju.user.service;

import com.minju.whitemonday.user.dto.SignupRequestDto;
import com.minju.whitemonday.user.entity.User;
import com.minju.whitemonday.user.dto.UserRoleEnum;
import com.minju.whitemonday.user.util.EncryptionUtil;
import com.minju.whitemonday.user.util.JwtUtil;
import com.minju.whitemonday.user.repository.UserRepository;
import com.minju.whitemonday.user.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionUtil encryptionUtil;
    private final VerificationTokenRepository verificationTokenRepository;
    private final JavaMailSender mailSender;
    private final JwtUtil jwtUtil;
    private final LogoutService logoutService;

    // ADMIN_TOKEN
    private final String ADMIN_TOKEN = "AAABnvxRVklrnYxKZ0aHgTBcXukeZygoC";

    // 회원가입
    public void signup(SignupRequestDto requestDto) {
        log.info("Starting user signup for username: {}", requestDto.getUsername());

        String username = requestDto.getUsername();
        String password = passwordEncoder.encode(requestDto.getPassword());
        String email = encryptionUtil.encrypt(requestDto.getEmail());
        String address = encryptionUtil.encrypt(requestDto.getAddress());
        String name = encryptionUtil.encrypt(requestDto.getName());

        log.info("Encrypted data - Email: {}, Address: {}, Name: {}", email, address, name);

        // 회원 중복 확인
        Optional<User> checkUsername = userRepository.findByUsername(username);
        if (checkUsername.isPresent()) {
            log.error("Username already exists: {}", requestDto.getUsername());
            throw new IllegalArgumentException("중복된 사용자가 존재합니다.");
        }

        // Email 중복확인
        Optional<User> checkEmail = userRepository.findByEmail(email);
        if (checkEmail.isPresent()) {
            throw new IllegalArgumentException("중복된 Email 입니다.");
        }

        // 사용자 ROLE 확인
        UserRoleEnum role = UserRoleEnum.USER;
        if (requestDto.isAdmin()) {
            if (!ADMIN_TOKEN.equals(requestDto.getAdminToken())) {
                throw new IllegalArgumentException("관리자 암호가 틀려 등록이 불가능합니다.");
            }
            role = UserRoleEnum.ADMIN;
        }

        // 사용자 등록
        User user = new User(username, password, email, role);
        user.setAddress(address);
        user.setName(name);
        userRepository.save(user);
        log.info("User saved successfully with username: {}", requestDto.getUsername());
    }

    // 로그인
    public String login(String username, String password) {
        log.info("Starting login process for username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Invalid username: {}", username);
                    return new IllegalArgumentException("사용자를 찾을 수 없습니다.");
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.error("Invalid password for username: {}", username);
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // JWT 생성
        String token = jwtUtil.createToken(username, user.getRole());
        log.info("Generated JWT for username: {}", username);

        return token;
    }

    // 로그아웃
    public void logout(String token) {
        log.info("Logging out token: {}", token);
        logoutService.invalidateToken(token);
        log.info("Token invalidated successfully.");
    }
}
