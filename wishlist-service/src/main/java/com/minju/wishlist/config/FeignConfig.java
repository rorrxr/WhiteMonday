package com.minju.wishlist.config;

import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL; // 요청 및 응답 로그 모두 활성화
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new UserFeignErrorDecoder();
    }
}