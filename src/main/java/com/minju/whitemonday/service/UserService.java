package com.minju.whitemonday.service;

import com.minju.whitemonday.dto.SignupRequestDto;
import com.minju.whitemonday.entity.User;
import com.minju.whitemonday.entity.UserRoleEnum;
import com.minju.whitemonday.entity.VerificationToken;
import com.minju.whitemonday.jwt.EncryptionUtil;
import com.minju.whitemonday.repository.UserRepository;
import com.minju.whitemonday.repository.VerificationTokenRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionUtil encryptionUtil;
    private final VerificationTokenRepository verificationTokenRepository;
    private final JavaMailSender mailSender;

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

        // email 중복확인
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
}