package com.minju.common.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Circuit Breaker 모니터링 REST Controller
 * Circuit Breaker 상태를 API로 확인 가능
 */
@RestController
@Slf4j
class CircuitBreakerMonitoringController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerMonitoringController(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @GetMapping("/api/circuit-breaker/status")
    public Map<String, Object> getCircuitBreakerStatus() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> circuitBreakers = new HashMap<>();

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            Map<String, Object> status = new HashMap<>();
            status.put("state", cb.getState().toString());
            status.put("failureRate", cb.getMetrics().getFailureRate());
            status.put("bufferedCalls", cb.getMetrics().getNumberOfBufferedCalls());
            status.put("failedCalls", cb.getMetrics().getNumberOfFailedCalls());
            status.put("successfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
            status.put("slowCalls", cb.getMetrics().getNumberOfSlowCalls());

            circuitBreakers.put(cb.getName(), status);
        });

        response.put("circuitBreakers", circuitBreakers);
        response.put("timestamp", System.currentTimeMillis());
        response.put("totalCircuitBreakers", circuitBreakers.size());

        log.debug("📊 Circuit Breaker status requested - {} circuit breakers found", circuitBreakers.size());

        return response;
    }

    @GetMapping("/api/circuit-breaker/metrics")
    public Map<String, Object> getCircuitBreakerMetrics() {
        Map<String, Object> response = new HashMap<>();

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            Map<String, Object> metrics = new HashMap<>();
            CircuitBreaker.Metrics cbMetrics = cb.getMetrics();

            metrics.put("state", cb.getState().toString());
            metrics.put("failureRate", cbMetrics.getFailureRate());
            metrics.put("slowCallRate", cbMetrics.getSlowCallRate());
            metrics.put("bufferedCalls", cbMetrics.getNumberOfBufferedCalls());
            metrics.put("failedCalls", cbMetrics.getNumberOfFailedCalls());
            metrics.put("successfulCalls", cbMetrics.getNumberOfSuccessfulCalls());
            metrics.put("slowCalls", cbMetrics.getNumberOfSlowCalls());
            metrics.put("notPermittedCalls", cbMetrics.getNumberOfNotPermittedCalls());

            response.put(cb.getName(), metrics);
        });

        return response;
    }
}
