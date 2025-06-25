package com.minju.product.service;

import com.minju.common.dto.StockResponse;
import com.minju.common.exception.InsufficientStockException;
import com.minju.common.exception.ProductNotFoundException;
import com.minju.common.exception.StockProcessingException;
import com.minju.common.exception.StockServiceException;
import com.minju.common.kafka.PaymentCompletedEvent;
import com.minju.product.entity.Product;
import com.minju.product.repository.ProductRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final ProductRepository productRepository;
    private final StockService stockService;

    @Retry(name = "stock-operation", fallbackMethod = "decreaseStockFallback")
    @CircuitBreaker(name = "redis-operation", fallbackMethod = "decreaseStockCircuitFallback")
    public StockResponse decreaseStock(Long productId, int quantity) {
        String lockKey = "stock:lock:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                String stockKey = "stock:" + productId;
                Integer currentStock = (Integer) redisTemplate.opsForValue().get(stockKey);

                if (currentStock == null || currentStock < quantity) {
                    throw new InsufficientStockException("재고가 부족합니다.");
                }

                redisTemplate.opsForValue().set(stockKey, currentStock - quantity);

                return StockResponse.builder()
                        .productId(productId)
                        .availableStock(currentStock - quantity)
                        .build();
            } else {
                throw new StockProcessingException("재고 처리 중 락 획득 실패");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Retry(name = "redis-operation", fallbackMethod = "getStockFromDbFallback")
    public StockResponse getStock(Long productId) {
        String stockKey = "product:stock:" + productId;
        Integer stock = (Integer) redisTemplate.opsForValue().get(stockKey);

        if (stock == null) {
            // Redis에서 조회 실패 시 DB에서 조회
            return getStockFromDatabase(productId);
        }

        return StockResponse.builder()
                .productId(productId)
                .availableStock(stock)
                .build();
    }

    // Fallback 메서드들
    public StockResponse decreaseStockFallback(Long productId, int quantity, Exception ex) {
        log.error("재고 감소 재시도 실패: productId={}, quantity={}, error={}",
                productId, quantity, ex.getMessage());
        throw new StockServiceException("재고 처리 서비스가 일시적으로 사용할 수 없습니다.");
    }

    public StockResponse getStockFromDbFallback(Long productId, Exception ex) {
        log.warn("Redis 재고 조회 실패, DB에서 조회: productId={}", productId);
        return getStockFromDatabase(productId);
    }

    private StockResponse getStockFromDatabase(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다."));

        return StockResponse.builder()
                .productId(productId)
                .availableStock(product.getStock())
                .build();
    }

    @KafkaListener(topics = "payment-completed-topic", groupId = "stock-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("재고 차감 이벤트 수신: {}", event);
        try {
            stockService.decreaseStock(Long.parseLong(String.valueOf(event.getProductId())), event.getQuantity());
            log.info("재고 차감 성공: 주문ID={}, 상품ID={}", event.getOrderId(), event.getProductId());
        } catch (Exception e) {
            log.error("재고 차감 실패: {}", e.getMessage());
        }
    }
}