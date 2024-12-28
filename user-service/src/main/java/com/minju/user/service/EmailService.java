package com.minju.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationToken(String email, String token) {
        String subject = "이메일 인증 Email Verification";
        String content = "다음 인증 토큰을 사용하여 이메일을 인증하세요. Please use the following token to verify your email. : " + token;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(content, false);
            mailSender.send(message);
            log.info("인증 이메일이 전송되었습니다. Verification email sent to {}", email);

        } catch (MessagingException e) {
            throw new RuntimeException("이메일 전송에 실패하였습니다 Failed to send email", e);
        }
    }
}