package com.minju.product.service;

import com.minju.product.entity.Product;
import com.minju.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * StockService Lua Script 방식 단위 테스트
 * - RLock 대신 Lua Script를 사용한 원자적 재고 관리 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockService Lua Script 테스트")
class StockServiceLuaScriptTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RedisScript<Long> decreaseStockScript;

    @Mock
    private RedisScript<Long> restoreStockScript;

    @Mock
    private RedisScript<Long> decreaseStockWithRateLimitScript;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private StockService stockService;

    private static final String STOCK_KEY_PREFIX = "product:stock:";
    private static final String RATE_LIMIT_KEY_PREFIX = "rate:";

    @BeforeEach
    void setUp() {
        stockService = new StockService(
                redisTemplate,
                productRepository,
                decreaseStockScript,
                restoreStockScript,
                decreaseStockWithRateLimitScript
        );
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("Lua Script 재고 차감 테스트")
    class DecreaseStockWithLuaScriptTest {

        @Test
        @DisplayName("Lua Script로 재고 차감 성공")
        void decreaseStock_withLuaScript_success() {
            // given
            Long productId = 1L;
            int quantity = 5;
            String stockKey = STOCK_KEY_PREFIX + productId;

            given(valueOperations.get(stockKey)).willReturn(100);
            given(redisTemplate.execute(
                    eq(decreaseStockScript),
                    eq(Collections.singletonList(stockKey)),
                    eq(String.valueOf(quantity))
            )).willReturn(95L);  // 100 - 5 = 95

            // when
            boolean result = stockService.decreaseStockWithTransaction(productId, quantity);

            // then
            assertThat(result).isTrue();
            verify(redisTemplate).execute(
                    eq(decreaseStockScript),
                    eq(Collections.singletonList(stockKey)),
                    eq(String.valueOf(quantity))
            );
        }

        @Test
        @DisplayName("Lua Script 재고 부족 시 실패 (-1 반환)")
        void decreaseStock_withLuaScript_outOfStock() {
            // given
            Long productId = 1L;
            int quantity = 150;
            String stockKey = STOCK_KEY_PREFIX + productId;

            given(valueOperations.get(stockKey)).willReturn(100);
            given(redisTemplate.execute(
                    eq(decreaseStockScript),
                    eq(Collections.singletonList(stockKey)),
                    eq(String.valueOf(quantity))
            )).willReturn(-1L);  // 재고 부족

            // when
            boolean result = stockService.decreaseStockWithTransaction(productId, quantity);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Lua Script 상품 없음 시 실패 (-2 반환)")
        void decreaseStock_withLuaScript_productNotFound() {
            // given
            Long productId = 999L;
            int quantity = 5;
            String stockKey = STOCK_KEY_PREFIX + productId;

            given(valueOperations.get(stockKey)).willReturn(null);
            given(productRepository.findById(productId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> stockService.decreaseStockWithTransaction(productId, quantity))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("Lua Script 실행 결과 null 시 실패")
        void decreaseStock_withLuaScript_nullResult() {
            // given
            Long productId = 1L;
            int quantity = 5;
            String stockKey = STOCK_KEY_PREFIX + productId;

            given(valueOperations.get(stockKey)).willReturn(100);
            given(redisTemplate.execute(
                    eq(decreaseStockScript),
                    eq(Collections.singletonList(stockKey)),
                    eq(String.valueOf(quantity))
            )).willReturn(null);

            // when
            boolean result = stockService.decreaseStockWithTransaction(productId, quantity);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Redis에 재고 없으면 DB에서 로드 후 차감")
        void decreaseStock_loadFromDbWhenNotInRedis() {
            // given
            Long productId = 1L;
            int quantity = 5;
            String stockKey = STOCK_KEY_PREFIX + productId;

            Product product = new Product();
            product.setId(productId);
            product.setStock(100);

            given(valueOperations.get(stockKey)).willReturn(null);
            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(redisTemplate.execute(
                    eq(decreaseStockScript),
                    eq(Collections.singletonList(stockKey)),
                    eq(String.valueOf(quantity))
            )).willReturn(95L);

            // when
            boolean result = stockService.decreaseStockWithTransaction(productId, quantity);

            // then
            assertThat(result).isTrue();
            verify(productRepository).findById(productId);
            verify(valueOperations).set(stockKey, 100);
        }
    }

    @Nested
    @DisplayName("Rate Limit 포함 재고 차감 테스트")
    class DecreaseStockWithRateLimitTest {

        @Test
        @DisplayName("Rate Limit 포함 재고 차감 성공")
        void decreaseStockWithRateLimit_success() {
            // given
            Long productId = 1L;
            Long userId = 100L;
            int quantity = 5;
            String stockKey = STOCK_KEY_PREFIX + productId;
            String rateLimitKey = RATE_LIMIT_KEY_PREFIX + userId + ":" + productId;

            given(valueOperations.get(stockKey)).willReturn(100);
            given(redisTemplate.execute(
                    eq(decreaseStockWithRateLimitScript),
                    eq(List.of(stockKey, rateLimitKey)),
                    eq(String.valueOf(quantity)),
                    eq(String.valueOf(5)),  // DEFAULT_RATE_LIMIT
                    eq(String.valueOf(60))  // RATE_LIMIT_EXPIRE_SECONDS
            )).willReturn(95L);

            // when
            boolean result = stockService.decreaseStockWithRateLimit(productId, userId, quantity);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Rate Limit 초과 시 예외 발생 (-3 반환)")
        void decreaseStockWithRateLimit_rateLimitExceeded() {
            // given
            Long productId = 1L;
            Long userId = 100L;
            int quantity = 5;
            String stockKey = STOCK_KEY_PREFIX + productId;
            String rateLimitKey = RATE_LIMIT_KEY_PREFIX + userId + ":" + productId;

            given(valueOperations.get(stockKey)).willReturn(100);
            given(redisTemplate.execute(
                    eq(decreaseStockWithRateLimitScript),
                    eq(List.of(stockKey, rateLimitKey)),
                    eq(String.valueOf(quantity)),
                    eq(String.valueOf(5)),
                    eq(String.valueOf(60))
            )).willReturn(-3L);  // Rate Limit 초과

            // when & then
            assertThatThrownBy(() -> stockService.decreaseStockWithRateLimit(productId, userId, quantity))
                    .isInstanceOf(StockService.RateLimitExceededException.class)
                    .hasMessageContaining("요청 제한을 초과했습니다");
        }

        @Test
        @DisplayName("Rate Limit + 재고 부족 시 실패")
        void decreaseStockWithRateLimit_outOfStock() {
            // given
            Long productId = 1L;
            Long userId = 100L;
            int quantity = 150;
            String stockKey = STOCK_KEY_PREFIX + productId;
            String rateLimitKey = RATE_LIMIT_KEY_PREFIX + userId + ":" + productId;

            given(valueOperations.get(stockKey)).willReturn(100);
            given(redisTemplate.execute(
                    eq(decreaseStockWithRateLimitScript),
                    eq(List.of(stockKey, rateLimitKey)),
                    eq(String.valueOf(quantity)),
                    eq(String.valueOf(5)),
                    eq(String.valueOf(60))
            )).willReturn(-1L);  // 재고 부족

            // when
            boolean result = stockService.decreaseStockWithRateLimit(productId, userId, quantity);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Rate Limit + 상품 없음 시 실패")
        void decreaseStockWithRateLimit_productNotFound() {
            // given
            Long productId = 999L;
            Long userId = 100L;
            int quantity = 5;
            String stockKey = STOCK_KEY_PREFIX + productId;
            String rateLimitKey = RATE_LIMIT_KEY_PREFIX + userId + ":" + productId;

            given(valueOperations.get(stockKey)).willReturn(100);
            given(redisTemplate.execute(
                    eq(decreaseStockWithRateLimitScript),
                    eq(List.of(stockKey, rateLimitKey)),
                    eq(String.valueOf(quantity)),
                    eq(String.valueOf(5)),
                    eq(String.valueOf(60))
            )).willReturn(-2L);  // 상품 없음

            // when
            boolean result = stockService.decreaseStockWithRateLimit(productId, userId, quantity);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Lua Script 재고 복구 테스트")
    class RestoreStockWithLuaScriptTest {

        @Test
        @DisplayName("Lua Script로 재고 복구 성공")
        void restoreStock_withLuaScript_success() {
            // given
            Long productId = 1L;
            int quantity = 10;
            String stockKey = STOCK_KEY_PREFIX + productId;

            Product product = new Product();
            product.setId(productId);
            product.setStock(90);

            given(redisTemplate.execute(
                    eq(restoreStockScript),
                    eq(Collections.singletonList(stockKey)),
                    eq(String.valueOf(quantity))
            )).willReturn(100L);  // 90 + 10 = 100

            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when
            stockService.restoreStock(productId, quantity);

            // then
            verify(redisTemplate).execute(
                    eq(restoreStockScript),
                    eq(Collections.singletonList(stockKey)),
                    eq(String.valueOf(quantity))
            );
            verify(productRepository).save(product);
            assertThat(product.getStock()).isEqualTo(100);  // 90 + 10
        }

        @Test
        @DisplayName("재고 복구 시 DB에도 반영")
        void restoreStock_updatesDatabase() {
            // given
            Long productId = 1L;
            int quantity = 10;
            String stockKey = STOCK_KEY_PREFIX + productId;

            Product product = new Product();
            product.setId(productId);
            product.setStock(90);

            given(redisTemplate.execute(
                    eq(restoreStockScript),
                    eq(Collections.singletonList(stockKey)),
                    eq(String.valueOf(quantity))
            )).willReturn(100L);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when
            stockService.restoreStock(productId, quantity);

            // then
            verify(productRepository).findById(productId);
            verify(productRepository).save(product);
        }
    }

    @Nested
    @DisplayName("재고 조회 테스트")
    class GetStockTest {

        @Test
        @DisplayName("Redis에서 재고 조회 성공")
        void getAccurateStock_fromRedis() {
            // given
            Long productId = 1L;
            String stockKey = STOCK_KEY_PREFIX + productId;

            given(valueOperations.get(stockKey)).willReturn(100);

            // when
            int stock = stockService.getAccurateStock(productId);

            // then
            assertThat(stock).isEqualTo(100);
            verify(productRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Redis에 없으면 DB에서 조회 후 캐싱")
        void getAccurateStock_fromDatabase() {
            // given
            Long productId = 1L;
            String stockKey = STOCK_KEY_PREFIX + productId;

            Product product = new Product();
            product.setId(productId);
            product.setStock(50);

            given(valueOperations.get(stockKey)).willReturn(null);
            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when
            int stock = stockService.getAccurateStock(productId);

            // then
            assertThat(stock).isEqualTo(50);
            verify(productRepository).findById(productId);
            verify(valueOperations).set(stockKey, 50);
        }
    }

    @Nested
    @DisplayName("Fallback 메서드 테스트")
    class FallbackTest {

        @Test
        @DisplayName("재고 감소 Fallback - DB에서 처리")
        void decreaseStockFallback_success() {
            // given
            Long productId = 1L;
            int quantity = 5;

            Product product = new Product();
            product.setId(productId);
            product.setStock(100);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when
            boolean result = stockService.decreaseStockFallback(productId, quantity, new RuntimeException("Redis 오류"));

            // then
            assertThat(result).isTrue();
            assertThat(product.getStock()).isEqualTo(95);
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("Rate Limit Fallback - RateLimitException은 그대로 throw")
        void decreaseStockWithRateLimitFallback_throwsRateLimitException() {
            // given
            Long productId = 1L;
            Long userId = 100L;
            int quantity = 5;
            StockService.RateLimitExceededException originalException =
                    new StockService.RateLimitExceededException("Rate Limit 초과");

            // when & then
            assertThatThrownBy(() ->
                    stockService.decreaseStockWithRateLimitFallback(productId, userId, quantity, originalException))
                    .isInstanceOf(StockService.RateLimitExceededException.class);
        }
    }

    @Nested
    @DisplayName("DB 직접 처리 테스트")
    class DirectDatabaseOperationTest {

        @Test
        @DisplayName("DB에서 직접 재고 감소")
        void decreaseStockInDatabase_success() {
            // given
            Long productId = 1L;
            int quantity = 10;

            Product product = new Product();
            product.setId(productId);
            product.setStock(100);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when
            boolean result = stockService.decreaseStockInDatabase(productId, quantity);

            // then
            assertThat(result).isTrue();
            assertThat(product.getStock()).isEqualTo(90);
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("DB에서 재고 부족 시 실패")
        void decreaseStockInDatabase_outOfStock() {
            // given
            Long productId = 1L;
            int quantity = 150;

            Product product = new Product();
            product.setId(productId);
            product.setStock(100);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when
            boolean result = stockService.decreaseStockInDatabase(productId, quantity);

            // then
            assertThat(result).isFalse();
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("DB에서 재고 복구")
        void restoreStockInDatabase_success() {
            // given
            Long productId = 1L;
            int quantity = 10;

            Product product = new Product();
            product.setId(productId);
            product.setStock(90);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when
            stockService.restoreStockInDatabase(productId, quantity);

            // then
            assertThat(product.getStock()).isEqualTo(100);
            verify(productRepository).save(product);
        }
    }

    @Nested
    @DisplayName("StockResponse 반환 테스트")
    class StockResponseTest {

        @Test
        @DisplayName("decreaseStock 성공 시 SUCCESS 반환")
        void decreaseStock_returnsSuccessResponse() {
            // given
            Long productId = 1L;
            int quantity = 5;
            String stockKey = STOCK_KEY_PREFIX + productId;

            given(valueOperations.get(stockKey)).willReturn(100);
            given(redisTemplate.execute(
                    eq(decreaseStockScript),
                    eq(Collections.singletonList(stockKey)),
                    eq(String.valueOf(quantity))
            )).willReturn(95L);

            // when
            var response = stockService.decreaseStock(productId, quantity);

            // then
            assertThat(response.getProductId()).isEqualTo(productId);
            assertThat(response.getQuantity()).isEqualTo(quantity);
            assertThat(response.getStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("decreaseStock 실패 시 FAILURE 반환")
        void decreaseStock_returnsFailureResponse() {
            // given
            Long productId = 1L;
            int quantity = 150;
            String stockKey = STOCK_KEY_PREFIX + productId;

            given(valueOperations.get(stockKey)).willReturn(100);
            given(redisTemplate.execute(
                    eq(decreaseStockScript),
                    eq(Collections.singletonList(stockKey)),
                    eq(String.valueOf(quantity))
            )).willReturn(-1L);

            // when
            var response = stockService.decreaseStock(productId, quantity);

            // then
            assertThat(response.getStatus()).isEqualTo("FAILURE");
        }
    }
}
