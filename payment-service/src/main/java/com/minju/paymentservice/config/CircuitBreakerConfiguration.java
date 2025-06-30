package com.minju.paymentservice.config;


import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * WhiteMonday Circuit Breaker Configuration
 * Order Service와 Payment Service에서 Product Service 호출 시 사용
 */
@Slf4j
@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public CircuitBreakerConfig productServiceCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
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
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig productServiceCircuitBreakerConfig) {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(productServiceCircuitBreakerConfig);
        registry.circuitBreaker("productService", productServiceCircuitBreakerConfig);

        log.info("Circuit Breaker Registry configured with productService");
        return registry;
    }

    @Bean
    public CircuitBreaker productServiceCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("productService");
        addEventListeners(circuitBreaker);
        return circuitBreaker;
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