package com.minju.order.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minju.common.idempotency.ProcessedEventRepository;
import com.minju.common.kafka.payment.PaymentCompletedEvent;
import com.minju.common.kafka.payment.PaymentFailedEvent;
import com.minju.common.kafka.stock.StockReservationFailedEvent;
import com.minju.common.kafka.stock.StockReservedEvent;
import com.minju.common.outbox.OutboxEvent;
import com.minju.common.outbox.OutboxEventRepository;
import com.minju.order.entity.OrderItem;
import com.minju.order.entity.Orders;
import com.minju.order.repository.OrderRepository;
import com.minju.order.saga.SagaOrchestrator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Saga 통합 테스트")
class SagaIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("orderdb")
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
    private SagaOrchestrator sagaOrchestrator;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DiscoveryClient discoveryClient;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private Orders testOrder;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        processedEventRepository.deleteAll();
        orderRepository.deleteAll();

        testOrder = new Orders();
        testOrder.setUserId(1L);
        testOrder.setOrderStatus("PENDING");
        testOrder.setTotalAmount(50000);
        testOrder.setTotalItemCount(2);
        testOrder.setReservedItemCount(0);
        testOrder.setFailedItemCount(0);

        OrderItem item1 = new OrderItem();
        item1.setProductId(1L);
        item1.setQuantity(2);
        item1.setPrice(15000);
        item1.setOrder(testOrder);
        testOrder.getOrderItems().add(item1);

        OrderItem item2 = new OrderItem();
        item2.setProductId(2L);
        item2.setQuantity(1);
        item2.setPrice(20000);
        item2.setOrder(testOrder);
        testOrder.getOrderItems().add(item2);

        testOrder = orderRepository.save(testOrder);
    }

    @Nested
    @DisplayName("재고 예약 성공 이벤트 처리")
    class HandleStockReservedTest {

        @Test
        @DisplayName("첫 번째 상품 예약 성공 시 카운터만 증가한다")
        void handleFirstProductReserved_ShouldOnlyIncrementCounter() {
            // given
            StockReservedEvent event = StockReservedEvent.builder()
                    .orderId(String.valueOf(testOrder.getId()))
                    .productId("1")
                    .quantity(2)
                    .status("STOCK_RESERVED")
                    .build();

            // when
            sagaOrchestrator.handleStockReserved(event);

            // then
            Orders updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getReservedItemCount()).isEqualTo(1);
            assertThat(updatedOrder.getOrderStatus()).isEqualTo("PENDING");

            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).isEmpty();
        }

        @Test
        @DisplayName("모든 상품 예약 완료 시 결제 요청 이벤트가 Outbox에 저장된다")
        void handleAllProductsReserved_ShouldSavePaymentRequestToOutbox() {
            // given
            testOrder.setReservedItemCount(1);
            orderRepository.save(testOrder);

            StockReservedEvent event = StockReservedEvent.builder()
                    .orderId(String.valueOf(testOrder.getId()))
                    .productId("2")
                    .quantity(1)
                    .status("STOCK_RESERVED")
                    .build();

            // when
            sagaOrchestrator.handleStockReserved(event);

            // then
            Orders updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getReservedItemCount()).isEqualTo(2);
            assertThat(updatedOrder.getOrderStatus()).isEqualTo("STOCK_RESERVED");

            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);
            assertThat(outboxEvents.get(0).getEventType()).isEqualTo("PAYMENT_REQUESTED");
            assertThat(outboxEvents.get(0).getTopic()).isEqualTo("payment-requested-topic");
        }

        @Test
        @DisplayName("중복 이벤트는 멱등성 처리로 무시된다")
        void handleDuplicateEvent_ShouldBeIgnored() {
            // given
            StockReservedEvent event = StockReservedEvent.builder()
                    .orderId(String.valueOf(testOrder.getId()))
                    .productId("1")
                    .quantity(2)
                    .status("STOCK_RESERVED")
                    .build();

            // when - 동일 이벤트 두 번 처리
            sagaOrchestrator.handleStockReserved(event);
            sagaOrchestrator.handleStockReserved(event);

            // then - 카운터가 1만 증가해야 함
            Orders updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getReservedItemCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("재고 예약 실패 이벤트 처리")
    class HandleStockReservationFailedTest {

        @Test
        @DisplayName("재고 예약 실패 시 주문이 CANCELLED 상태가 되고 Outbox에 취소 이벤트가 저장된다")
        void handleStockReservationFailed_ShouldCancelOrder() {
            // given
            StockReservationFailedEvent event = StockReservationFailedEvent.builder()
                    .orderId(String.valueOf(testOrder.getId()))
                    .productId("1")
                    .quantity(2)
                    .reason("재고 부족")
                    .status("STOCK_RESERVATION_FAILED")
                    .build();

            // when
            sagaOrchestrator.handleStockReservationFailed(event);

            // then
            Orders updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getOrderStatus()).isEqualTo("CANCELLED");
            assertThat(updatedOrder.getFailedItemCount()).isEqualTo(1);

            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents.stream()
                    .anyMatch(e -> e.getEventType().equals("ORDER_CANCELLED")))
                    .isTrue();
        }

        @Test
        @DisplayName("이미 예약된 상품이 있으면 재고 복구 이벤트도 저장된다")
        void handleStockReservationFailed_WithReservedItems_ShouldRestoreStock() {
            // given
            testOrder.setReservedItemCount(1);
            orderRepository.save(testOrder);

            StockReservationFailedEvent event = StockReservationFailedEvent.builder()
                    .orderId(String.valueOf(testOrder.getId()))
                    .productId("2")
                    .quantity(1)
                    .reason("재고 부족")
                    .status("STOCK_RESERVATION_FAILED")
                    .build();

            // when
            sagaOrchestrator.handleStockReservationFailed(event);

            // then
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents.stream()
                    .anyMatch(e -> e.getEventType().equals("STOCK_RESTORE_REQUESTED")))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("결제 완료 이벤트 처리")
    class HandlePaymentCompletedTest {

        @Test
        @DisplayName("결제 성공 시 주문이 COMPLETED 상태가 된다")
        void handlePaymentSuccess_ShouldCompleteOrder() {
            // given
            testOrder.setOrderStatus("STOCK_RESERVED");
            orderRepository.save(testOrder);

            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .orderId(String.valueOf(testOrder.getId()))
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .amount(50000)
                    .success(true)
                    .status("PAYMENT_COMPLETED")
                    .build();

            // when
            sagaOrchestrator.handlePaymentCompleted(event);

            // then
            Orders updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getOrderStatus()).isEqualTo("COMPLETED");

            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents.stream()
                    .anyMatch(e -> e.getEventType().equals("ORDER_COMPLETED")))
                    .isTrue();
        }

        @Test
        @DisplayName("결제 실패 시 보상 트랜잭션(재고 복구)이 발행된다")
        void handlePaymentFailed_ShouldTriggerCompensation() {
            // given
            testOrder.setOrderStatus("STOCK_RESERVED");
            orderRepository.save(testOrder);

            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .orderId(String.valueOf(testOrder.getId()))
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .amount(50000)
                    .success(false)
                    .status("PAYMENT_COMPLETED")
                    .build();

            // when
            sagaOrchestrator.handlePaymentCompleted(event);

            // then
            Orders updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getOrderStatus()).isEqualTo("PAYMENT_FAILED");

            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents.stream()
                    .anyMatch(e -> e.getEventType().equals("STOCK_RESTORE_REQUESTED")))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("결제 실패 이벤트 처리")
    class HandlePaymentFailedTest {

        @Test
        @DisplayName("결제 실패 이벤트 수신 시 보상 트랜잭션이 실행된다")
        void handlePaymentFailed_ShouldExecuteCompensation() {
            // given
            testOrder.setOrderStatus("STOCK_RESERVED");
            orderRepository.save(testOrder);

            PaymentFailedEvent event = PaymentFailedEvent.builder()
                    .orderId(String.valueOf(testOrder.getId()))
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .reason("카드 한도 초과")
                    .status("PAYMENT_FAILED")
                    .build();

            // when
            sagaOrchestrator.handlePaymentFailed(event);

            // then
            Orders updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getOrderStatus()).isEqualTo("PAYMENT_FAILED");

            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents.stream()
                    .anyMatch(e -> e.getEventType().equals("STOCK_RESTORE_REQUESTED")))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("다중 상품 Saga 동기화 테스트")
    class MultiItemSagaSyncTest {

        @Test
        @DisplayName("3개 상품 중 2개 예약 완료, 1개 실패 시 모든 예약 취소")
        void multiItemSaga_PartialFailure_ShouldCancelAll() {
            // given
            Orders multiItemOrder = new Orders();
            multiItemOrder.setUserId(1L);
            multiItemOrder.setOrderStatus("PENDING");
            multiItemOrder.setTotalAmount(100000);
            multiItemOrder.setTotalItemCount(3);
            multiItemOrder.setReservedItemCount(0);
            multiItemOrder.setFailedItemCount(0);

            for (int i = 1; i <= 3; i++) {
                OrderItem item = new OrderItem();
                item.setProductId((long) i);
                item.setQuantity(1);
                item.setPrice(33333);
                item.setOrder(multiItemOrder);
                multiItemOrder.getOrderItems().add(item);
            }
            multiItemOrder = orderRepository.save(multiItemOrder);
            String orderId = String.valueOf(multiItemOrder.getId());

            // when - 2개 예약 성공
            sagaOrchestrator.handleStockReserved(StockReservedEvent.builder()
                    .orderId(orderId).productId("1").quantity(1).status("STOCK_RESERVED").build());
            sagaOrchestrator.handleStockReserved(StockReservedEvent.builder()
                    .orderId(orderId).productId("2").quantity(1).status("STOCK_RESERVED").build());

            // when - 1개 예약 실패
            sagaOrchestrator.handleStockReservationFailed(StockReservationFailedEvent.builder()
                    .orderId(orderId).productId("3").quantity(1).reason("재고 부족").status("STOCK_RESERVATION_FAILED").build());

            // then
            Orders updatedOrder = orderRepository.findById(multiItemOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getOrderStatus()).isEqualTo("CANCELLED");
            assertThat(updatedOrder.getReservedItemCount()).isEqualTo(2);
            assertThat(updatedOrder.getFailedItemCount()).isEqualTo(1);

            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            long restoreEventCount = outboxEvents.stream()
                    .filter(e -> e.getEventType().equals("STOCK_RESTORE_REQUESTED"))
                    .count();
            assertThat(restoreEventCount).isGreaterThanOrEqualTo(2);
        }
    }
}
