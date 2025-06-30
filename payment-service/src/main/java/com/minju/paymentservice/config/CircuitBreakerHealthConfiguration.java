package com.minju.paymentservice.config;


import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Circuit Breaker Health Indicator
 * Actuator Health Check에 Circuit Breaker 상태 포함
 */
@Slf4j
@Configuration
public class CircuitBreakerHealthConfiguration {

    @Bean
    public HealthIndicator circuitBreakerHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerHealthIndicator(circuitBreakerRegistry);
    }

    private static class CircuitBreakerHealthIndicator implements HealthIndicator {
        private final CircuitBreakerRegistry circuitBreakerRegistry;

        public CircuitBreakerHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry) {
            this.circuitBreakerRegistry = circuitBreakerRegistry;
        }

        @Override
        public Health health() {
            Map<String, Object> details = new HashMap<>();
            boolean allHealthy = true;

            for (CircuitBreaker circuitBreaker : circuitBreakerRegistry.getAllCircuitBreakers()) {
                String name = circuitBreaker.getName();
                CircuitBreaker.State state = circuitBreaker.getState();

                Map<String, Object> cbDetails = new HashMap<>();
                cbDetails.put("state", state.toString());
                cbDetails.put("failureRate", String.format("%.2f%%", circuitBreaker.getMetrics().getFailureRate()));
                cbDetails.put("bufferedCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
                cbDetails.put("failedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
                cbDetails.put("successfulCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());

                details.put(name, cbDetails);

                if (state == CircuitBreaker.State.OPEN) {
                    allHealthy = false;
                    log.warn("🔴 Circuit Breaker '{}' is in OPEN state", name);
                }
            }

            if (allHealthy) {
                return Health.up()
                        .withDetail("circuitBreakers", details)
                        .withDetail("message", "All circuit breakers are healthy")
                        .build();
            } else {
                return Health.down()
                        .withDetail("circuitBreakers", details)
                        .withDetail("message", "One or more circuit breakers are open")
                        .build();
            }
        }
    }
}
