package com.minju.product.service;

import com.minju.common.dto.StockResponse;
import com.minju.product.entity.Product;
import com.minju.product.repository.ProductRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Lua Script 기반 재고 관리 서비스
 * - RLock 대신 Lua Script로 원자적 연산 수행
 * - 네트워크 왕복 최소화
 * - 완전 원자적 재고 차감/복구
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;

    // Lua Script Beans
    private final RedisScript<Long> decreaseStockScript;
    private final RedisScript<Long> restoreStockScript;
    private final RedisScript<Long> decreaseStockWithRateLimitScript;

    private static final String STOCK_KEY_PREFIX = "product:stock:";
    private static final String RATE_LIMIT_KEY_PREFIX = "rate:";

    // Lua Script 반환 코드
    private static final long RESULT_OUT_OF_STOCK = -1L;
    private static final long RESULT_PRODUCT_NOT_FOUND = -2L;
    private static final long RESULT_RATE_LIMIT_EXCEEDED = -3L;

    // Rate Limit 설정
    private static final int DEFAULT_RATE_LIMIT = 5;  // 기본 요청 제한 횟수
    private static final int RATE_LIMIT_EXPIRE_SECONDS = 60;  // 제한 시간 (초)

    /**
     * Lua Script를 사용한 재고 차감 (원자적 연산)
     * - 락 없이 완전 원자적 처리
     * - 네트워크 왕복 1회
     */
    @Retry(name = "stock-operation", fallbackMethod = "decreaseStockFallback")
    @CircuitBreaker(name = "redis-operation", fallbackMethod = "decreaseStockCircuitFallback")
    @Transactional
    public boolean decreaseStockWithTransaction(Long productId, int quantity) {
        String stockKey = STOCK_KEY_PREFIX + productId;

        try {
            // Redis에 재고가 없으면 DB에서 로드
            ensureStockInRedis(productId);

            // Lua Script 실행 (원자적 재고 차감)
            Long result = redisTemplate.execute(
                    decreaseStockScript,
                    Collections.singletonList(stockKey),
                    String.valueOf(quantity)
            );

            if (result == null) {
                log.error("Lua Script 실행 실패 - productId: {}", productId);
                return false;
            }

            if (result == RESULT_OUT_OF_STOCK) {
                log.warn("재고 부족 - productId: {}, 요청수량: {}", productId, quantity);
                return false;
            }

            if (result == RESULT_PRODUCT_NOT_FOUND) {
                log.warn("상품 재고 키 없음 - productId: {}", productId);
                return false;
            }

            log.info("재고 감소 성공 (Lua) - productId: {}, 감소수량: {}, 남은재고: {}",
                    productId, quantity, result);
            return true;

        } catch (Exception e) {
            log.error("재고 감소 처리 중 오류: ", e);
            throw e;
        }
    }

    /**
     * Rate Limit 포함 재고 차감
     * - 사용자별 요청 제한 + 재고 차감을 원자적으로 처리
     */
    @Retry(name = "stock-operation", fallbackMethod = "decreaseStockWithRateLimitFallback")
    @CircuitBreaker(name = "redis-operation", fallbackMethod = "decreaseStockWithRateLimitCircuitFallback")
    @Transactional
    public boolean decreaseStockWithRateLimit(Long productId, Long userId, int quantity) {
        String stockKey = STOCK_KEY_PREFIX + productId;
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + userId + ":" + productId;

        try {
            ensureStockInRedis(productId);

            Long result = redisTemplate.execute(
                    decreaseStockWithRateLimitScript,
                    List.of(stockKey, rateLimitKey),
                    String.valueOf(quantity),
                    String.valueOf(DEFAULT_RATE_LIMIT),
                    String.valueOf(RATE_LIMIT_EXPIRE_SECONDS)
            );

            if (result == null) {
                log.error("Lua Script 실행 실패 - productId: {}", productId);
                return false;
            }

            switch (result.intValue()) {
                case -1:
                    log.warn("재고 부족 - productId: {}, 요청수량: {}", productId, quantity);
                    return false;
                case -2:
                    log.warn("상품 재고 키 없음 - productId: {}", productId);
                    return false;
                case -3:
                    log.warn("Rate Limit 초과 - userId: {}, productId: {}", userId, productId);
                    throw new RateLimitExceededException("요청 제한을 초과했습니다. 잠시 후 다시 시도해주세요.");
                default:
                    log.info("재고 감소 성공 (Lua+RateLimit) - productId: {}, userId: {}, 남은재고: {}",
                            productId, userId, result);
                    return true;
            }

        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("재고 감소 처리 중 오류: ", e);
            throw e;
        }
    }

    /**
     * Lua Script를 사용한 재고 복구 (원자적 연산)
     */
    @Transactional
    public void restoreStock(Long productId, int quantity) {
        String stockKey = STOCK_KEY_PREFIX + productId;

        try {
            // Lua Script 실행 (원자적 재고 복구)
            Long newStock = redisTemplate.execute(
                    restoreStockScript,
                    Collections.singletonList(stockKey),
                    String.valueOf(quantity)
            );

            log.info("재고 복구 완료 (Lua) - productId: {}, 복구수량: {}, 현재재고: {}",
                    productId, quantity, newStock);

            // DB에도 반영
            restoreStockInDatabase(productId, quantity);

        } catch (Exception e) {
            log.error("재고 복구 실패: ", e);
            throw new RuntimeException("재고 복구 실패", e);
        }
    }

    /**
     * Redis에 재고가 없으면 DB에서 로드
     */
    private void ensureStockInRedis(Long productId) {
        String stockKey = STOCK_KEY_PREFIX + productId;
        Object stock = redisTemplate.opsForValue().get(stockKey);

        if (stock == null) {
            int dbStock = getAndCacheStockFromDatabase(productId);
            log.info("DB에서 재고 로드 - productId: {}, stock: {}", productId, dbStock);
        }
    }

    @Retry(name = "redis-operation", fallbackMethod = "getStockFromDbFallback")
    @CircuitBreaker(name = "redis-operation", fallbackMethod = "getStockCircuitFallback")
    public int getAccurateStock(Long productId) {
        String stockKey = STOCK_KEY_PREFIX + productId;

        try {
            Object stockObj = redisTemplate.opsForValue().get(stockKey);
            if (stockObj != null) {
                return ((Number) stockObj).intValue();
            }

            return getAndCacheStockFromDatabase(productId);

        } catch (Exception e) {
            log.error("Redis에서 재고 조회 실패: ", e);
            throw e;
        }
    }

    @Retry(name = "database-operation")
    private int getAndCacheStockFromDatabase(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + productId));

        int stock = product.getStock();
        String stockKey = STOCK_KEY_PREFIX + productId;
        redisTemplate.opsForValue().set(stockKey, stock);

        return stock;
    }

    // ==================== Fallback Methods ====================

    public boolean decreaseStockFallback(Long productId, int quantity, Exception ex) {
        log.error("재고 감소 재시도 실패 - productId: {}, error: {}", productId, ex.getMessage());
        try {
            return decreaseStockInDatabase(productId, quantity);
        } catch (Exception dbEx) {
            log.error("DB에서도 재고 감소 실패: ", dbEx);
            return false;
        }
    }

    public boolean decreaseStockCircuitFallback(Long productId, int quantity, Exception ex) {
        log.error("재고 감소 Circuit Breaker 활성화 - productId: {}", productId);
        return decreaseStockInDatabase(productId, quantity);
    }

    public boolean decreaseStockWithRateLimitFallback(Long productId, Long userId, int quantity, Exception ex) {
        log.error("Rate Limit 재고 감소 실패 - productId: {}, userId: {}", productId, userId);
        if (ex instanceof RateLimitExceededException) {
            throw (RateLimitExceededException) ex;
        }
        return decreaseStockInDatabase(productId, quantity);
    }

    public boolean decreaseStockWithRateLimitCircuitFallback(Long productId, Long userId, int quantity, Exception ex) {
        log.error("Rate Limit 재고 감소 Circuit Breaker 활성화 - productId: {}", productId);
        if (ex instanceof RateLimitExceededException) {
            throw (RateLimitExceededException) ex;
        }
        return decreaseStockInDatabase(productId, quantity);
    }

    public int getStockFromDbFallback(Long productId, Exception ex) {
        log.warn("Redis 재고 조회 실패, DB에서 조회 - productId: {}", productId);
        return getStockFromDatabase(productId);
    }

    public int getStockCircuitFallback(Long productId, Exception ex) {
        log.error("재고 조회 Circuit Breaker 활성화 - productId: {}", productId);
        return getStockFromDatabase(productId);
    }

    // ==================== DB 직접 처리 ====================

    public StockResponse decreaseStock(Long productId, int quantity) {
        boolean success = decreaseStockWithTransaction(productId, quantity);
        return new StockResponse(productId, quantity, success ? "SUCCESS" : "FAILURE");
    }

    @Transactional
    public boolean decreaseStockInDatabase(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + productId));

        if (product.getStock() >= quantity) {
            product.setStock(product.getStock() - quantity);
            productRepository.save(product);
            log.info("DB 재고 감소 완료 - productId: {}, 감소수량: {}", productId, quantity);
            return true;
        }
        return false;
    }

    private int getStockFromDatabase(Long productId) {
        return productRepository.findById(productId)
                .map(Product::getStock)
                .orElse(0);
    }

    @Transactional
    public void restoreStockInDatabase(Long productId, int quantity) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + productId));

            product.setStock(product.getStock() + quantity);
            productRepository.save(product);
            log.info("DB 재고 복구 완료 - productId: {}, 복구수량: {}", productId, quantity);

        } catch (Exception e) {
            log.error("DB 재고 복구 실패: ", e);
        }
    }

    // ==================== Custom Exception ====================

    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
