package com.minju.common.config;


import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class FeignConfig {
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // SecurityContextHolder 또는 CustomContext에서 사용자 정보를 가져와 헤더에 추가
            String userId = "PLACEHOLDER_USER_ID"; // 실제 사용자 ID 가져오기 로직 필요
            requestTemplate.header("X-User-Id", userId);
        };
    }
}
