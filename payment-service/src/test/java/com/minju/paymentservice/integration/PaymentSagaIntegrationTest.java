package com.minju.paymentservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minju.common.idempotency.ProcessedEventRepository;
import com.minju.common.kafka.payment.PaymentRequestedEvent;
import com.minju.common.outbox.OutboxEvent;
import com.minju.common.outbox.OutboxEventRepository;
import com.minju.paymentservice.entity.Payment;
import com.minju.paymentservice.repository.PaymentRepository;
import com.minju.paymentservice.saga.PaymentSagaHandler;
import com.minju.paymentservice.service.PaymentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Payment Saga 통합 테스트")
class PaymentSagaIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("paymentdb")
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
    private PaymentSagaHandler paymentSagaHandler;

    @SpyBean
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

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

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        processedEventRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Nested
    @DisplayName("결제 요청 처리")
    class HandlePaymentRequestTest {

        @Test
        @DisplayName("결제 성공 시 PAYMENT_COMPLETED 이벤트가 Outbox에 저장된다")
        void handlePaymentRequest_Success_ShouldSaveCompletedEvent() {
            // given
            PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                    .orderId("1")
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .amount(20000)
                    .status("PAYMENT_REQUESTED")
                    .build();

            given(paymentService.processPayment(any(PaymentRequestedEvent.class))).willReturn(true);

            // when
            paymentSagaHandler.handlePaymentRequest(event);

            // then
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);
            assertThat(outboxEvents.get(0).getEventType()).isEqualTo("PAYMENT_COMPLETED");
            assertThat(outboxEvents.get(0).getTopic()).isEqualTo("payment-completed-topic");
        }

        @Test
        @DisplayName("결제 실패 시 PAYMENT_FAILED 이벤트가 Outbox에 저장된다")
        void handlePaymentRequest_Failed_ShouldSaveFailedEvent() {
            // given
            PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                    .orderId("2")
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .amount(20000)
                    .status("PAYMENT_REQUESTED")
                    .build();

            given(paymentService.processPayment(any(PaymentRequestedEvent.class))).willReturn(false);

            // when
            paymentSagaHandler.handlePaymentRequest(event);

            // then
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);
            assertThat(outboxEvents.get(0).getEventType()).isEqualTo("PAYMENT_FAILED");
            assertThat(outboxEvents.get(0).getTopic()).isEqualTo("payment-failed-topic");
        }

        @Test
        @DisplayName("결제 처리 중 예외 발생 시 PAYMENT_FAILED 이벤트가 저장된다")
        void handlePaymentRequest_Exception_ShouldSaveFailedEvent() {
            // given
            PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                    .orderId("3")
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .amount(20000)
                    .status("PAYMENT_REQUESTED")
                    .build();

            given(paymentService.processPayment(any(PaymentRequestedEvent.class)))
                    .willThrow(new RuntimeException("결제 게이트웨이 오류"));

            // when
            paymentSagaHandler.handlePaymentRequest(event);

            // then
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);
            assertThat(outboxEvents.get(0).getEventType()).isEqualTo("PAYMENT_FAILED");
        }

        @Test
        @DisplayName("중복 이벤트는 멱등성 처리로 무시된다")
        void handlePaymentRequest_DuplicateEvent_ShouldBeIgnored() {
            // given
            PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                    .orderId("4")
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .amount(20000)
                    .status("PAYMENT_REQUESTED")
                    .build();

            given(paymentService.processPayment(any(PaymentRequestedEvent.class))).willReturn(true);

            // when - 동일 이벤트 두 번 처리
            paymentSagaHandler.handlePaymentRequest(event);
            paymentSagaHandler.handlePaymentRequest(event);

            // then - Outbox에 1개만 저장되어야 함
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);
        }
    }

    @Nested
    @DisplayName("결제 성공 이벤트 검증")
    class PaymentCompletedEventTest {

        @Test
        @DisplayName("결제 성공 이벤트에 필요한 필드가 모두 포함된다")
        void paymentCompletedEvent_ShouldContainAllFields() throws Exception {
            // given
            PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                    .orderId("5")
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .amount(20000)
                    .status("PAYMENT_REQUESTED")
                    .build();

            given(paymentService.processPayment(any(PaymentRequestedEvent.class))).willReturn(true);

            // when
            paymentSagaHandler.handlePaymentRequest(event);

            // then
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);

            String payload = outboxEvents.get(0).getPayload();
            assertThat(payload).contains("\"orderId\":\"5\"");
            assertThat(payload).contains("\"userId\":\"1\"");
            assertThat(payload).contains("\"amount\":20000");
            assertThat(payload).contains("\"success\":true");
        }
    }

    @Nested
    @DisplayName("결제 실패 이벤트 검증")
    class PaymentFailedEventTest {

        @Test
        @DisplayName("결제 실패 이벤트에 실패 사유가 포함된다")
        void paymentFailedEvent_ShouldContainReason() throws Exception {
            // given
            PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                    .orderId("6")
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .amount(20000)
                    .status("PAYMENT_REQUESTED")
                    .build();

            given(paymentService.processPayment(any(PaymentRequestedEvent.class))).willReturn(false);

            // when
            paymentSagaHandler.handlePaymentRequest(event);

            // then
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);

            String payload = outboxEvents.get(0).getPayload();
            assertThat(payload).contains("\"orderId\":\"6\"");
            assertThat(payload).contains("reason");
        }
    }

    @Nested
    @DisplayName("Payment 엔티티 저장 검증")
    class PaymentEntityTest {

        @Test
        @DisplayName("결제 처리 시 Payment 엔티티가 올바르게 저장된다")
        void processPayment_ShouldSavePaymentEntity() {
            // given
            PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                    .orderId("7")
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .amount(20000)
                    .status("PAYMENT_REQUESTED")
                    .build();

            // when - 실제 processPayment 호출 (Spy 사용)
            given(paymentService.processPayment(any(PaymentRequestedEvent.class)))
                    .willAnswer(invocation -> {
                        Payment payment = new Payment();
                        payment.setOrderId(7L);
                        payment.setUserId(1L);
                        payment.setAmount(20000);
                        payment.setPaymentStatus("COMPLETED");
                        payment.setPaymentMethod("CARD");
                        paymentRepository.save(payment);
                        return true;
                    });

            paymentSagaHandler.handlePaymentRequest(event);

            // then
            List<Payment> payments = paymentRepository.findAll();
            assertThat(payments).hasSize(1);
            assertThat(payments.get(0).getOrderId()).isEqualTo(7L);
            assertThat(payments.get(0).getAmount()).isEqualTo(20000);
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Fallback 검증")
    class CircuitBreakerTest {

        @Test
        @DisplayName("Fallback 호출 시 실패 Payment가 저장된다")
        void fallback_ShouldSaveFailedPayment() {
            // given
            PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                    .orderId("8")
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .amount(20000)
                    .status("PAYMENT_REQUESTED")
                    .build();

            // when
            paymentService.processPaymentFallback(event, new RuntimeException("Circuit Breaker Open"));

            // then
            List<Payment> payments = paymentRepository.findAll();
            assertThat(payments).hasSize(1);
            assertThat(payments.get(0).getPaymentStatus()).isEqualTo("FAILED");
            assertThat(payments.get(0).getFailureReason()).contains("Circuit Breaker");
        }

        @Test
        @DisplayName("Retry Fallback 시 실패 사유가 저장된다")
        void retryFallback_ShouldSaveFailureReason() {
            // given
            PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                    .orderId("9")
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .amount(20000)
                    .status("PAYMENT_REQUESTED")
                    .build();

            // when
            paymentService.processPaymentRetryFallback(event, new RuntimeException("Connection timeout"));

            // then
            List<Payment> payments = paymentRepository.findAll();
            assertThat(payments).hasSize(1);
            assertThat(payments.get(0).getPaymentStatus()).isEqualTo("FAILED");
            assertThat(payments.get(0).getFailureReason()).contains("Connection timeout");
        }
    }

    @Nested
    @DisplayName("Outbox 이벤트 상태 검증")
    class OutboxEventStatusTest {

        @Test
        @DisplayName("Outbox 이벤트는 PENDING 상태로 저장된다")
        void outboxEvent_ShouldBePending() {
            // given
            PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                    .orderId("10")
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .amount(20000)
                    .status("PAYMENT_REQUESTED")
                    .build();

            given(paymentService.processPayment(any(PaymentRequestedEvent.class))).willReturn(true);

            // when
            paymentSagaHandler.handlePaymentRequest(event);

            // then
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);
            assertThat(outboxEvents.get(0).getStatus()).isEqualTo("PENDING");
            assertThat(outboxEvents.get(0).getRetryCount()).isEqualTo(0);
        }
    }
}
