package com.minju.product.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minju.common.dlq.DeadLetterEvent;
import com.minju.common.dlq.DeadLetterEventRepository;
import com.minju.common.idempotency.ProcessedEventRepository;
import com.minju.common.kafka.stock.StockReservationRequestEvent;
import com.minju.common.kafka.stock.StockRestoreEvent;
import com.minju.common.outbox.OutboxEvent;
import com.minju.common.outbox.OutboxEventRepository;
import com.minju.product.entity.Product;
import com.minju.product.repository.ProductRepository;
import com.minju.product.saga.StockSagaHandler;
import com.minju.product.service.StockService;
import org.junit.jupiter.api.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Stock Saga 통합 테스트")
class StockSagaIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("productdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private StockSagaHandler stockSagaHandler;

    @Autowired
    private StockService stockService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private DeadLetterEventRepository deadLetterEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DiscoveryClient discoveryClient;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private RedissonClient redissonClient;

    private Product testProduct;
    private RLock mockLock;
    private ValueOperations<String, Object> valueOperations;
    private AtomicInteger mockStock;

    @BeforeEach
    void setUp() throws InterruptedException {
        outboxEventRepository.deleteAll();
        processedEventRepository.deleteAll();
        deadLetterEventRepository.deleteAll();
        productRepository.deleteAll();

        testProduct = new Product();
        testProduct.setTitle("테스트 상품");
        testProduct.setPrice(10000);
        testProduct.setStock(100);
        testProduct = productRepository.save(testProduct);

        // Mock Redis 설정
        mockStock = new AtomicInteger(100);
        mockLock = mock(RLock.class);
        valueOperations = mock(ValueOperations.class);

        given(redissonClient.getLock(anyString())).willReturn(mockLock);
        given(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
        given(mockLock.isHeldByCurrentThread()).willReturn(true);
        doNothing().when(mockLock).unlock();

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willAnswer(inv -> mockStock.get());
        given(valueOperations.decrement(anyString(), anyLong())).willAnswer(inv -> {
            long amount = inv.getArgument(1);
            return (long) mockStock.addAndGet((int) -amount);
        });
        given(valueOperations.increment(anyString(), anyLong())).willAnswer(inv -> {
            long amount = inv.getArgument(1);
            return (long) mockStock.addAndGet((int) amount);
        });
    }

    @Nested
    @DisplayName("재고 예약 요청 처리")
    class HandleStockReservationRequestTest {

        @Test
        @DisplayName("재고가 충분하면 STOCK_RESERVED 이벤트가 Outbox에 저장된다")
        void handleRequest_SufficientStock_ShouldSaveReservedEvent() {
            // given
            StockReservationRequestEvent event = StockReservationRequestEvent.builder()
                    .orderId("1")
                    .productId(String.valueOf(testProduct.getId()))
                    .quantity(10)
                    .status("STOCK_RESERVATION_REQUESTED")
                    .build();

            // when
            stockSagaHandler.handleStockReservationRequest(event);

            // then
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);
            assertThat(outboxEvents.get(0).getEventType()).isEqualTo("STOCK_RESERVED");
            assertThat(outboxEvents.get(0).getTopic()).isEqualTo("stock-reserved-topic");
            assertThat(mockStock.get()).isEqualTo(90);
        }

        @Test
        @DisplayName("재고가 부족하면 STOCK_RESERVATION_FAILED 이벤트가 Outbox에 저장된다")
        void handleRequest_InsufficientStock_ShouldSaveFailedEvent() {
            // given
            StockReservationRequestEvent event = StockReservationRequestEvent.builder()
                    .orderId("1")
                    .productId(String.valueOf(testProduct.getId()))
                    .quantity(200)
                    .status("STOCK_RESERVATION_REQUESTED")
                    .build();

            // when
            stockSagaHandler.handleStockReservationRequest(event);

            // then
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);
            assertThat(outboxEvents.get(0).getEventType()).isEqualTo("STOCK_RESERVATION_FAILED");
            assertThat(outboxEvents.get(0).getTopic()).isEqualTo("stock-reservation-failed-topic");
        }

        @Test
        @DisplayName("중복 이벤트는 멱등성 처리로 무시된다")
        void handleRequest_DuplicateEvent_ShouldBeIgnored() {
            // given
            StockReservationRequestEvent event = StockReservationRequestEvent.builder()
                    .orderId("1")
                    .productId(String.valueOf(testProduct.getId()))
                    .quantity(10)
                    .status("STOCK_RESERVATION_REQUESTED")
                    .build();

            // when - 동일 이벤트 두 번 처리
            stockSagaHandler.handleStockReservationRequest(event);
            stockSagaHandler.handleStockReservationRequest(event);

            // then - Outbox에 1개만 저장되어야 함
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);
        }

        @Test
        @DisplayName("트랜잭션 실패 시 STOCK_RESERVATION_FAILED 이벤트가 저장된다")
        void handleRequest_TransactionFailed_ShouldSaveFailedEvent() throws InterruptedException {
            // given
            given(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(false);

            StockReservationRequestEvent event = StockReservationRequestEvent.builder()
                    .orderId("1")
                    .productId(String.valueOf(testProduct.getId()))
                    .quantity(10)
                    .status("STOCK_RESERVATION_REQUESTED")
                    .build();

            // when
            stockSagaHandler.handleStockReservationRequest(event);

            // then
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);
            assertThat(outboxEvents.get(0).getEventType()).isEqualTo("STOCK_RESERVATION_FAILED");
        }
    }

    @Nested
    @DisplayName("재고 복구 요청 처리")
    class HandleStockRestoreTest {

        @Test
        @DisplayName("재고 복구 성공 시 재고가 증가한다")
        void handleRestore_Success_ShouldIncreaseStock() {
            // given
            mockStock.set(90);

            StockRestoreEvent event = StockRestoreEvent.builder()
                    .orderId("1")
                    .productId(String.valueOf(testProduct.getId()))
                    .quantity(10)
                    .reason("결제 실패")
                    .status("STOCK_RESTORE_REQUESTED")
                    .build();

            // when
            stockSagaHandler.handleStockRestore(event);

            // then
            assertThat(mockStock.get()).isEqualTo(100);

            Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            assertThat(updatedProduct.getStock()).isEqualTo(110);
        }

        @Test
        @DisplayName("중복 복구 이벤트는 멱등성 처리로 무시된다")
        void handleRestore_DuplicateEvent_ShouldBeIgnored() {
            // given
            StockRestoreEvent event = StockRestoreEvent.builder()
                    .orderId("1")
                    .productId(String.valueOf(testProduct.getId()))
                    .quantity(10)
                    .reason("결제 실패")
                    .status("STOCK_RESTORE_REQUESTED")
                    .build();

            // when - 동일 이벤트 두 번 처리
            stockSagaHandler.handleStockRestore(event);
            int stockAfterFirst = mockStock.get();
            stockSagaHandler.handleStockRestore(event);
            int stockAfterSecond = mockStock.get();

            // then - 두 번째 처리에서는 재고가 변하지 않아야 함
            assertThat(stockAfterFirst).isEqualTo(stockAfterSecond);
        }

        @Test
        @DisplayName("복구 실패 시 DLQ에 저장된다")
        void handleRestore_Failed_ShouldSaveToDLQ() throws InterruptedException {
            // given
            given(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                    .willThrow(new RuntimeException("락 획득 실패"));

            StockRestoreEvent event = StockRestoreEvent.builder()
                    .orderId("1")
                    .productId(String.valueOf(testProduct.getId()))
                    .quantity(10)
                    .reason("결제 실패")
                    .status("STOCK_RESTORE_REQUESTED")
                    .build();

            // when
            stockSagaHandler.handleStockRestore(event);

            // then
            List<DeadLetterEvent> dlqEvents = deadLetterEventRepository.findAll();
            assertThat(dlqEvents).hasSize(1);
            assertThat(dlqEvents.get(0).getEventType()).isEqualTo("StockRestoreEvent");
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Fallback 테스트")
    class CircuitBreakerFallbackTest {

        @Test
        @DisplayName("재고 예약 Fallback 시 STOCK_RESERVATION_FAILED가 저장된다")
        void reservationFallback_ShouldSaveFailedEvent() {
            // given
            StockReservationRequestEvent event = StockReservationRequestEvent.builder()
                    .orderId("1")
                    .productId(String.valueOf(testProduct.getId()))
                    .quantity(10)
                    .build();

            // when
            stockSagaHandler.handleStockReservationFallback(event, new RuntimeException("Circuit Breaker Open"));

            // then
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);
            assertThat(outboxEvents.get(0).getEventType()).isEqualTo("STOCK_RESERVATION_FAILED");
        }

        @Test
        @DisplayName("재고 복구 Fallback 시 DLQ에 저장된다")
        void restoreFallback_ShouldSaveToDLQ() {
            // given
            StockRestoreEvent event = StockRestoreEvent.builder()
                    .orderId("1")
                    .productId(String.valueOf(testProduct.getId()))
                    .quantity(10)
                    .reason("결제 실패")
                    .build();

            // when
            stockSagaHandler.handleStockRestoreFallback(event, new RuntimeException("Circuit Breaker Open"));

            // then
            List<DeadLetterEvent> dlqEvents = deadLetterEventRepository.findAll();
            assertThat(dlqEvents).hasSize(1);
        }
    }

    @Nested
    @DisplayName("동시성 테스트")
    class ConcurrencyTest {

        @Test
        @DisplayName("동시에 여러 재고 예약 요청이 들어와도 정확하게 처리된다")
        void concurrentReservations_ShouldBeProcessedCorrectly() throws InterruptedException {
            // given
            int threadCount = 10;
            int quantityPerRequest = 5;
            mockStock.set(100);

            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);

            // when
            for (int i = 0; i < threadCount; i++) {
                final int orderId = i;
                executor.submit(() -> {
                    try {
                        StockReservationRequestEvent event = StockReservationRequestEvent.builder()
                                .orderId(String.valueOf(orderId))
                                .productId(String.valueOf(testProduct.getId()))
                                .quantity(quantityPerRequest)
                                .status("STOCK_RESERVATION_REQUESTED")
                                .build();
                        stockSagaHandler.handleStockReservationRequest(event);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            // then
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(threadCount);

            long successCount = outboxEvents.stream()
                    .filter(e -> e.getEventType().equals("STOCK_RESERVED"))
                    .count();
            long failCount = outboxEvents.stream()
                    .filter(e -> e.getEventType().equals("STOCK_RESERVATION_FAILED"))
                    .count();

            assertThat(successCount + failCount).isEqualTo(threadCount);
            assertThat(mockStock.get()).isGreaterThanOrEqualTo(0);
        }
    }
}
