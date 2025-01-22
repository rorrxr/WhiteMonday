package com.minju.user.config;

import com.minju.user.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class EncryptionConfig {

    @Value("${encryption.secret-key}")
    private String secretKey;

    @Bean
    public EncryptionUtil encryptionUtil() {
        return new EncryptionUtil(secretKey);
    }
}
