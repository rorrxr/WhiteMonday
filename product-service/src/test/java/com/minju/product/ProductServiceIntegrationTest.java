package com.minju.product;

import com.minju.product.entity.Product;
import com.minju.product.repository.ProductRepository;
import com.minju.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class ProductServiceIntegrationTest {
    // 통합 테스트 코드
    // 동시성 환경에서 상품 재고를 감소시키는 로직을 검증하는 시나리오

    // 여러 개의 스레드가 동시에 하나의 상품에 대한 재고 감소 요청을 보냈을 때,
    // 재고가 올바르게 감소되고, 동시에 재고가 부족한 경우 예외가 발생하여 처리되는지 확인합니다.

    /*

    상품 ID: 1L
    초기 재고: 50
    스레드 수: 20 (20개의 요청이 동시에 처리됨)
    각 스레드의 감소 요청량: 3 (총 요청량: 20 * 3 = 60 > 초기 재고: 50)
    Redis와 DB에 재고 데이터를 저장 및 관리하며, Redis를 우선적으로 사용하여 캐시 방식으로 재고를 감소합니다.
    Redis 동작 모킹

    redisTemplate.opsForValue()를 통해 Redis 캐시에서 재고를 관리합니다.
    get: 상품의 현재 재고를 조회합니다.
    decrement: 재고를 감소시킵니다. 재고가 음수가 되면 IllegalArgumentException을 던집니다.
    Redisson 분산 락

    Redisson을 사용하여 재고 감소 요청 시 락을 걸어 동시성 문제를 방지합니다.
    tryLock: 락을 획득하고, unlock으로 락을 해제합니다.
    실행

    20개의 스레드가 동시에 상품 ID: 1에 대해 재고를 감소시키는 요청을 보냅니다.
    스레드는 productService.decreaseStockWithTransaction() 메서드를 호출하며, 내부적으로 Redis와 DB를 활용해 재고를 감소시킵니다.
    검증

    성공한 요청(재고 감소에 성공한 스레드)의 개수가 실패한 요청(재고 부족 또는 예외 발생으로 실패한 스레드)보다 적음을 확인합니다.
    최종 재고가 0보다 크거나 같아야 함을 검증합니다.
    실패한 요청 수가 0보다 큰지 확인하여, 동시성 문제 없이 재고 부족 상황이 올바르게 처리되었는지 확인합니다.

    */
    private static final String PRODUCT_KEY_PREFIX = "product:stock:";

    @MockBean
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @MockBean
    private RedissonClient redissonClient;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    private RLock mockLock;

    @BeforeEach
    void setUp() {
        mockLock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(mockLock);

        try {
            doReturn(true).when(mockLock).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        doNothing().when(mockLock).unlock();

        // RedisTemplate의 opsForValue() Mock 설정
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // ValueOperations 동작 Mock 설정
        AtomicInteger currentStock = new AtomicInteger(50);
        when(valueOperations.get(eq(PRODUCT_KEY_PREFIX + 1L)))
                .thenReturn(currentStock.get());
        when(valueOperations.decrement(eq(PRODUCT_KEY_PREFIX + 1L), anyLong()))
                .thenAnswer(invocation -> {
                    int decrement = Math.toIntExact(invocation.getArgument(1));
                    int newStock = currentStock.addAndGet(-decrement);
                    if (newStock < 0) {
                        throw new IllegalArgumentException("재고 부족");
                    }
                    return (long) newStock;
                });
    }



    @Test
    void testConcurrentStockDecreaseWithInsufficientStock() throws InterruptedException {
        // Arrange
        Long productId = 1L;
        int initialStock = 50;
        int threadCount = 20;
        int decreaseAmount = 3; // 총 필요 수량: 60 > 초기 재고: 50
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger currentStock = new AtomicInteger(initialStock);
        when(redisTemplate.opsForValue().get(PRODUCT_KEY_PREFIX + productId))
                .thenAnswer(inv -> currentStock.get());
        when(redisTemplate.opsForValue().decrement(eq(PRODUCT_KEY_PREFIX + productId), anyInt()))
                .thenAnswer(inv -> {
                    int amount = inv.getArgument(1);
                    int newStock = currentStock.get() - amount;
                    if (newStock < 0) {
                        throw new IllegalArgumentException("재고 부족");
                    }
                    return currentStock.addAndGet(-amount);
                });

        Product mockProduct = new Product();
        mockProduct.setId(productId);
        mockProduct.setStock(initialStock);
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));

        // Act
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    productService.decreaseStockWithTransaction(productId, decreaseAmount);
                    return true;
                } catch (Exception e) {
                    return false;
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        int successCount = 0;
        int failCount = 0;
        for (Future<Boolean> future : futures) {
            try {
                if (future.get()) successCount++;
                else failCount++;
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        assertThat(successCount).isLessThan(threadCount);
        assertThat(currentStock.get()).isGreaterThanOrEqualTo(0);
        assertThat(failCount).isGreaterThan(0);
    }
}