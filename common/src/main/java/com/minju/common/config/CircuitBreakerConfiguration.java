package com.minju.common.config;


import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * WhiteMonday Circuit Breaker Configuration
 * Order Service와 Payment Service에서 Product Service 호출 시 사용
 */
@Slf4j
@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        // Product Service 호출용 Circuit Breaker 설정
        CircuitBreakerConfig productServiceConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 50% 실패율
                .waitDurationInOpenState(Duration.ofSeconds(15)) // OPEN 상태 15초 유지
                .slidingWindowSize(10) // 10번의 호출을 기준으로 판단
                .minimumNumberOfCalls(5) // 최소 5번 호출 후 Circuit Breaker 활성화
                .permittedNumberOfCallsInHalfOpenState(3) // HALF-OPEN에서 3번 시도
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .recordExceptions(
                        Exception.class,
                        RuntimeException.class,
                        java.util.concurrent.TimeoutException.class,
                        java.net.ConnectException.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class,
                        IllegalStateException.class
                )
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(productServiceConfig);

        // productService 이름으로 Circuit Breaker 등록
        registry.circuitBreaker("productService", productServiceConfig);

        log.info("Circuit Breaker Registry configured with productService");
        return registry;
    }

    /**
     * Circuit Breaker 이벤트 리스너 등록
     */
    @PostConstruct
    public void registerEventListener() {
        CircuitBreakerRegistry registry = circuitBreakerRegistry();

        registry.getEventPublisher().onEntryAdded(event -> {
            CircuitBreaker circuitBreaker = event.getAddedEntry();
            addEventListeners(circuitBreaker);
        });

        // 기존 Circuit Breaker에도 리스너 추가
        registry.getAllCircuitBreakers().forEach(this::addEventListeners);
    }

    private void addEventListeners(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    log.warn("Circuit Breaker '{}' state transition: {} -> {} at {}",
                            event.getCircuitBreakerName(),
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState(),
                            event.getCreationTime());
                });

        circuitBreaker.getEventPublisher()
                .onFailureRateExceeded(event -> {
                    log.error("Circuit Breaker '{}' failure rate exceeded: {}%",
                            event.getCircuitBreakerName(),
                            event.getFailureRate());
                });

        circuitBreaker.getEventPublisher()
                .onCallNotPermitted(event -> {
                    log.warn("Circuit Breaker '{}' call not permitted - Circuit is OPEN",
                            event.getCircuitBreakerName());
                });

        circuitBreaker.getEventPublisher()
                .onError(event -> {
                    log.error("Circuit Breaker '{}' recorded error: {} - Duration: {}ms",
                            event.getCircuitBreakerName(),
                            event.getThrowable().getMessage(),
                            event.getElapsedDuration().toMillis());
                });

        circuitBreaker.getEventPublisher()
                .onSuccess(event -> {
                    log.debug("Circuit Breaker '{}' successful call - Duration: {}ms",
                            event.getCircuitBreakerName(),
                            event.getElapsedDuration().toMillis());
                });

        log.info("Event listeners registered for Circuit Breaker: {}", circuitBreaker.getName());
    }
}