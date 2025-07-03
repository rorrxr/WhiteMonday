package com.minju.product.service;

import com.minju.common.dto.StockResponse;
import com.minju.product.entity.Product;
import com.minju.product.repository.ProductRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final ProductRepository productRepository;

    private static final String STOCK_KEY_PREFIX = "product:stock:";

    @Retry(name = "stock-operation", fallbackMethod = "decreaseStockFallback")
    @CircuitBreaker(name = "redis-operation", fallbackMethod = "decreaseStockCircuitFallback")
    @Transactional
    public boolean decreaseStockWithTransaction(Long productId, int quantity) {
        String lockKey = "stock:lock:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 분산 락 획득 (3초 대기, 10초 유지)
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                return decreaseStockInternal(productId, quantity);
            } else {
                throw new RuntimeException("재고 처리 중 락 획득 실패");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("재고 처리 중 인터럽트 발생", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Retry(name = "redis-operation", fallbackMethod = "getStockFromDbFallback")
    @CircuitBreaker(name = "redis-operation", fallbackMethod = "getStockCircuitFallback")
    public int getAccurateStock(Long productId) {
        String stockKey = STOCK_KEY_PREFIX + productId;

        try {
            Integer stock = (Integer) redisTemplate.opsForValue().get(stockKey);
            if (stock != null) {
                return stock;
            }

            // Redis에 없으면 DB에서 조회 후 Redis에 저장
            return getAndCacheStockFromDatabase(productId);

        } catch (Exception e) {
            log.error("Redis에서 재고 조회 실패: ", e);
            throw e;
        }
    }

    private boolean decreaseStockInternal(Long productId, int quantity) {
        String stockKey = STOCK_KEY_PREFIX + productId;

        try {
            // Redis에서 재고 확인
            Integer currentStock = (Integer) redisTemplate.opsForValue().get(stockKey);

            if (currentStock == null) {
                // Redis에 재고 정보가 없으면 DB에서 조회
                currentStock = getAndCacheStockFromDatabase(productId);
            }

            if (currentStock < quantity) {
                log.warn("재고 부족 - productId: {}, 현재재고: {}, 요청수량: {}",
                        productId, currentStock, quantity);
                return false;
            }

            // 재고 감소
            Long newStock = redisTemplate.opsForValue().decrement(stockKey, quantity);

            if (newStock < 0) {
                // 음수가 되면 롤백
                redisTemplate.opsForValue().increment(stockKey, quantity);
                log.warn("재고 감소 후 음수 - 롤백 처리");
                return false;
            }

            // 주기적으로 DB 동기화 (별도 스케줄러에서 처리)
            log.info("재고 감소 성공 - productId: {}, 감소수량: {}, 남은재고: {}",
                    productId, quantity, newStock);
            return true;

        } catch (Exception e) {
            log.error("재고 감소 처리 중 오류: ", e);
            throw e;
        }
    }

    @Retry(name = "database-operation")
    private int getAndCacheStockFromDatabase(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + productId));

        int stock = product.getStock();

        // Redis에 캐시
        String stockKey = STOCK_KEY_PREFIX + productId;
        redisTemplate.opsForValue().set(stockKey, stock);

        return stock;
    }

    // Fallback Methods
    public boolean decreaseStockFallback(Long productId, int quantity, Exception ex) {
        log.error("재고 감소 재시도 실패 - productId: {}, error: {}", productId, ex.getMessage());

        // DB에서 직접 처리 시도
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

    public int getStockFromDbFallback(Long productId, Exception ex) {
        log.warn("Redis 재고 조회 실패, DB에서 조회 - productId: {}", productId);
        return getStockFromDatabase(productId);
    }

    public int getStockCircuitFallback(Long productId, Exception ex) {
        log.error("재고 조회 Circuit Breaker 활성화 - productId: {}", productId);
        return getStockFromDatabase(productId);
    }

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
    public void restoreStock(Long productId, int quantity) {
        String lockKey = "stock:lock:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                String stockKey = STOCK_KEY_PREFIX + productId;

                // Redis에서 재고 복구
                Long newStock = redisTemplate.opsForValue().increment(stockKey, quantity);
                log.info("재고 복구 완료 - productId: {}, 복구수량: {}, 현재재고: {}",
                        productId, quantity, newStock);

                // DB에도 반영
                restoreStockInDatabase(productId, quantity);

            } else {
                throw new RuntimeException("재고 복구 중 락 획득 실패");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("재고 복구 중 인터럽트 발생", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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
            // Redis는 복구되었지만 DB 복구 실패시 별도 처리 필요
        }
    }
}