//package com.minju.whitemonday.common.config;
//
//import com.minju.whitemonday.common.util.EncryptionUtil;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//
//@Slf4j
//@Configuration
//@EnableWebSecurity // Spring Security 지원을 가능하게 함
//@RequiredArgsConstructor
//public class EncryptionConfig {
//
//    @Value("${ENCRYPTION_SECRET_KEY}")
//    private String secretKey;
//
//    @Bean
//    public EncryptionUtil encryptionUtil() {
//        return new EncryptionUtil(secretKey);
//    }
//}
