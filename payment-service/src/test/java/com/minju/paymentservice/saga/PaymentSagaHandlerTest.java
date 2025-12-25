package com.minju.paymentservice.saga;

import com.minju.common.idempotency.ProcessedEventRepository;
import com.minju.common.kafka.payment.PaymentRequestedEvent;
import com.minju.paymentservice.outbox.OutboxEventPublisher;
import com.minju.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentSagaHandler 단위 테스트")
class PaymentSagaHandlerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private OutboxEventPublisher outboxPublisher;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private PaymentSagaHandler paymentSagaHandler;

    private PaymentRequestedEvent mockEvent;

    @BeforeEach
    void setUp() {
        mockEvent = PaymentRequestedEvent.builder()
                .orderId("1")
                .userId("1")
                .productId("1")
                .quantity(2)
                .amount(20000)
                .status("PAYMENT_REQUESTED")
                .build();
    }

    @Nested
    @DisplayName("결제 요청 처리")
    class HandlePaymentRequestTest {

        @Test
        @DisplayName("결제 성공 시 PAYMENT_COMPLETED 이벤트가 발행된다")
        void handlePaymentRequest_Success_ShouldPublishCompleted() {
            // given
            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(paymentService.processPayment(mockEvent)).willReturn(true);

            // when
            paymentSagaHandler.handlePaymentRequest(mockEvent);

            // then
            verify(outboxPublisher).saveEvent(
                    eq("PAYMENT"),
                    eq("1"),
                    eq("PAYMENT_COMPLETED"),
                    eq("payment-completed-topic"),
                    any()
            );
        }

        @Test
        @DisplayName("결제 실패 시 PAYMENT_FAILED 이벤트가 발행된다")
        void handlePaymentRequest_Failed_ShouldPublishFailed() {
            // given
            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(paymentService.processPayment(mockEvent)).willReturn(false);

            // when
            paymentSagaHandler.handlePaymentRequest(mockEvent);

            // then
            verify(outboxPublisher).saveEvent(
                    eq("PAYMENT"),
                    eq("1"),
                    eq("PAYMENT_FAILED"),
                    eq("payment-failed-topic"),
                    any()
            );
        }

        @Test
        @DisplayName("결제 처리 중 예외 발생 시 PAYMENT_FAILED 이벤트가 발행된다")
        void handlePaymentRequest_Exception_ShouldPublishFailed() {
            // given
            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(paymentService.processPayment(mockEvent))
                    .willThrow(new RuntimeException("결제 게이트웨이 오류"));

            // when
            paymentSagaHandler.handlePaymentRequest(mockEvent);

            // then
            verify(outboxPublisher).saveEvent(
                    eq("PAYMENT"),
                    eq("1"),
                    eq("PAYMENT_FAILED"),
                    eq("payment-failed-topic"),
                    any()
            );
        }

        @Test
        @DisplayName("중복 이벤트는 무시된다 (멱등성)")
        void handlePaymentRequest_DuplicateEvent_ShouldBeIgnored() {
            // given
            given(processedEventRepository.existsById(anyString())).willReturn(true);

            // when
            paymentSagaHandler.handlePaymentRequest(mockEvent);

            // then
            verify(paymentService, never()).processPayment(any());
            verify(outboxPublisher, never()).saveEvent(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("결제 성공 이벤트 검증")
    class PaymentCompletedEventTest {

        @Test
        @DisplayName("결제 성공 이벤트에 필요한 필드가 모두 포함된다")
        void paymentCompletedEvent_ShouldContainAllFields() {
            // given
            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(paymentService.processPayment(mockEvent)).willReturn(true);

            // when
            paymentSagaHandler.handlePaymentRequest(mockEvent);

            // then
            verify(outboxPublisher).saveEvent(
                    eq("PAYMENT"),
                    eq("1"),
                    eq("PAYMENT_COMPLETED"),
                    eq("payment-completed-topic"),
                    argThat(event -> {
                        // PaymentCompletedEvent 필드 검증
                        return event != null;
                    })
            );
        }
    }

    @Nested
    @DisplayName("결제 실패 이벤트 검증")
    class PaymentFailedEventTest {

        @Test
        @DisplayName("결제 실패 이벤트에 실패 사유가 포함된다")
        void paymentFailedEvent_ShouldContainReason() {
            // given
            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(paymentService.processPayment(mockEvent)).willReturn(false);

            // when
            paymentSagaHandler.handlePaymentRequest(mockEvent);

            // then
            verify(outboxPublisher).saveEvent(
                    eq("PAYMENT"),
                    eq("1"),
                    eq("PAYMENT_FAILED"),
                    eq("payment-failed-topic"),
                    argThat(event -> event != null)
            );
        }
    }
}
