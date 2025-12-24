package com.minju.product.service;

import com.minju.product.entity.Product;
import com.minju.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockService 단위 테스트")
class StockServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RLock rLock;

    @InjectMocks
    private StockService stockService;

    private Product mockProduct;

    @BeforeEach
    void setUp() {
        mockProduct = new Product();
        mockProduct.setId(1L);
        mockProduct.setTitle("테스트 상품");
        mockProduct.setPrice(10000);
        mockProduct.setStock(100);
    }

    @Nested
    @DisplayName("재고 조회 테스트")
    class GetStockTest {

        @Test
        @DisplayName("Redis에 재고가 있으면 Redis에서 조회한다")
        void getAccurateStock_ExistsInRedis_ShouldReturnFromRedis() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("product:stock:1")).willReturn(50);

            // when
            int stock = stockService.getAccurateStock(1L);

            // then
            assertThat(stock).isEqualTo(50);
            verify(productRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Redis에 재고가 없으면 DB에서 조회 후 캐싱한다")
        void getAccurateStock_NotInRedis_ShouldFetchFromDbAndCache() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("product:stock:1")).willReturn(null);
            given(productRepository.findById(1L)).willReturn(Optional.of(mockProduct));

            // when
            int stock = stockService.getAccurateStock(1L);

            // then
            assertThat(stock).isEqualTo(100);
            verify(valueOperations).set("product:stock:1", 100);
        }
    }

    @Nested
    @DisplayName("재고 차감 테스트")
    class DecreaseStockTest {

        @Test
        @DisplayName("재고가 충분하면 차감에 성공한다")
        void decreaseStockWithTransaction_SufficientStock_ShouldSucceed() throws InterruptedException {
            // given
            given(redissonClient.getLock("stock:lock:1")).willReturn(rLock);
            given(rLock.tryLock(3, 10, TimeUnit.SECONDS)).willReturn(true);
            given(rLock.isHeldByCurrentThread()).willReturn(true);

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("product:stock:1")).willReturn(100);
            given(valueOperations.decrement("product:stock:1", 10)).willReturn(90L);

            // when
            boolean result = stockService.decreaseStockWithTransaction(1L, 10);

            // then
            assertThat(result).isTrue();
            verify(rLock).unlock();
        }

        @Test
        @DisplayName("재고가 부족하면 차감에 실패한다")
        void decreaseStockWithTransaction_InsufficientStock_ShouldFail() throws InterruptedException {
            // given
            given(redissonClient.getLock("stock:lock:1")).willReturn(rLock);
            given(rLock.tryLock(3, 10, TimeUnit.SECONDS)).willReturn(true);
            given(rLock.isHeldByCurrentThread()).willReturn(true);

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("product:stock:1")).willReturn(5);

            // when
            boolean result = stockService.decreaseStockWithTransaction(1L, 10);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("락 획득 실패 시 예외가 발생한다")
        void decreaseStockWithTransaction_LockFailed_ShouldThrowException() throws InterruptedException {
            // given
            given(redissonClient.getLock("stock:lock:1")).willReturn(rLock);
            given(rLock.tryLock(3, 10, TimeUnit.SECONDS)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> stockService.decreaseStockWithTransaction(1L, 10))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("락 획득 실패");
        }

        @Test
        @DisplayName("차감 후 재고가 음수가 되면 롤백한다")
        void decreaseStockWithTransaction_NegativeStock_ShouldRollback() throws InterruptedException {
            // given
            given(redissonClient.getLock("stock:lock:1")).willReturn(rLock);
            given(rLock.tryLock(3, 10, TimeUnit.SECONDS)).willReturn(true);
            given(rLock.isHeldByCurrentThread()).willReturn(true);

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("product:stock:1")).willReturn(10);
            given(valueOperations.decrement("product:stock:1", 10)).willReturn(-5L);

            // when
            boolean result = stockService.decreaseStockWithTransaction(1L, 10);

            // then
            assertThat(result).isFalse();
            verify(valueOperations).increment("product:stock:1", 10);
        }
    }

    @Nested
    @DisplayName("재고 복구 테스트")
    class RestoreStockTest {

        @Test
        @DisplayName("재고 복구 시 Redis와 DB 모두 업데이트된다")
        void restoreStock_ShouldUpdateBothRedisAndDb() throws InterruptedException {
            // given
            given(redissonClient.getLock("stock:lock:1")).willReturn(rLock);
            given(rLock.tryLock(3, 10, TimeUnit.SECONDS)).willReturn(true);
            given(rLock.isHeldByCurrentThread()).willReturn(true);

            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.increment("product:stock:1", 5)).willReturn(105L);

            given(productRepository.findById(1L)).willReturn(Optional.of(mockProduct));
            given(productRepository.save(any(Product.class))).willReturn(mockProduct);

            // when
            stockService.restoreStock(1L, 5);

            // then
            verify(valueOperations).increment("product:stock:1", 5);
            verify(productRepository).save(any(Product.class));
            verify(rLock).unlock();
        }
    }

    @Nested
    @DisplayName("DB 직접 재고 처리 (Fallback)")
    class DatabaseFallbackTest {

        @Test
        @DisplayName("DB에서 직접 재고 차감이 가능하다")
        void decreaseStockInDatabase_SufficientStock_ShouldSucceed() {
            // given
            given(productRepository.findById(1L)).willReturn(Optional.of(mockProduct));
            given(productRepository.save(any(Product.class))).willReturn(mockProduct);

            // when
            boolean result = stockService.decreaseStockInDatabase(1L, 10);

            // then
            assertThat(result).isTrue();
            assertThat(mockProduct.getStock()).isEqualTo(90);
        }

        @Test
        @DisplayName("DB 재고 부족 시 차감 실패")
        void decreaseStockInDatabase_InsufficientStock_ShouldFail() {
            // given
            mockProduct.setStock(5);
            given(productRepository.findById(1L)).willReturn(Optional.of(mockProduct));

            // when
            boolean result = stockService.decreaseStockInDatabase(1L, 10);

            // then
            assertThat(result).isFalse();
        }
    }
}
