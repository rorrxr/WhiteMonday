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
        // 이메일 인증 토큰 생성
        VerificationToken token = new VerificationToken(requestDto.getEmail());
        verificationTokenRepository.save(token);

        // 이메일 발송
        sendVerificationEmail(requestDto.getEmail(), token.getToken());
    }

    private void sendVerificationEmail(String email, String token) {
        String subject = "이메일 인증";
        String url = "http://localhost:8080/api/v1/verify-email?token=" + token; // 토큰 검증 URL
        String content = "<p>회원가입을 완료하려면 아래 링크를 클릭하세요:</p>"
                + "<a href=\"" + url + "\">이메일 인증하기</a>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }
}
