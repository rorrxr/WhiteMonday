package com.minju.paymentservice.service;

import com.minju.common.kafka.payment.PaymentRequestedEvent;
import com.minju.paymentservice.dto.PaymentRequestDto;
import com.minju.paymentservice.entity.Payment;
import com.minju.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequestedEvent mockEvent;
    private Payment mockPayment;

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

        mockPayment = new Payment();
        mockPayment.setId(1L);
        mockPayment.setOrderId(1L);
        mockPayment.setUserId(1L);
        mockPayment.setAmount(20000);
        mockPayment.setPaymentStatus("PROCESSING");
        mockPayment.setPaymentMethod("CARD");
    }

    @Nested
    @DisplayName("결제 진입 검증 테스트")
    class ValidatePaymentEntryTest {

        @Test
        @DisplayName("결제 진행 중 상태이면 true를 반환한다")
        void validatePaymentEntry_ValidStatus_ShouldReturnTrue() {
            // given
            PaymentRequestDto dto = new PaymentRequestDto();
            dto.setPaymentStatus("결제 진행 중");

            // when
            boolean result = paymentService.validatePaymentEntry(dto);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("다른 상태이면 false를 반환한다")
        void validatePaymentEntry_InvalidStatus_ShouldReturnFalse() {
            // given
            PaymentRequestDto dto = new PaymentRequestDto();
            dto.setPaymentStatus("결제 완료");

            // when
            boolean result = paymentService.validatePaymentEntry(dto);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("결제 처리 테스트")
    class ProcessPaymentTest {

        @Test
        @DisplayName("결제 처리 시 Payment 엔티티가 저장된다")
        void processPayment_ShouldSavePaymentEntity() {
            // given
            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            given(paymentRepository.save(paymentCaptor.capture())).willAnswer(invocation -> {
                Payment payment = invocation.getArgument(0);
                payment.setId(1L);
                return payment;
            });

            // when
            // Note: 실제 테스트에서는 외부 PG 호출 시뮬레이션이 랜덤하므로
            // 결과 검증보다는 Payment 저장 로직 검증에 집중
            try {
                paymentService.processPayment(mockEvent);
            } catch (Exception e) {
                // 랜덤 실패 가능
            }

            // then
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getOrderId()).isEqualTo(1L);
            assertThat(savedPayment.getUserId()).isEqualTo(1L);
            assertThat(savedPayment.getAmount()).isEqualTo(20000);
            assertThat(savedPayment.getPaymentMethod()).isEqualTo("CARD");
        }

        @Test
        @DisplayName("결제 처리 시 초기 상태는 PROCESSING이다")
        void processPayment_InitialStatus_ShouldBeProcessing() {
            // given
            java.util.List<String> capturedStatuses = new java.util.ArrayList<>();
            given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
                Payment payment = invocation.getArgument(0);
                capturedStatuses.add(payment.getPaymentStatus());
                payment.setId(1L);
                return payment;
            });

            // when
            try {
                paymentService.processPayment(mockEvent);
            } catch (Exception e) {
                // 랜덤 실패 가능
            }

            // then - 첫 번째 저장 시점에는 PROCESSING 상태
            assertThat(capturedStatuses.get(0)).isEqualTo("PROCESSING");
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Fallback 테스트")
    class CircuitBreakerFallbackTest {

        @Test
        @DisplayName("Fallback 시 FAILED 상태로 저장된다")
        void processPaymentFallback_ShouldSaveFailedPayment() {
            // given
            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            given(paymentRepository.save(paymentCaptor.capture())).willReturn(mockPayment);

            // when
            boolean result = paymentService.processPaymentFallback(
                    mockEvent,
                    new RuntimeException("Circuit Breaker Open")
            );

            // then
            assertThat(result).isFalse();
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getPaymentStatus()).isEqualTo("FAILED");
            assertThat(savedPayment.getFailureReason()).contains("Circuit Breaker");
        }

        @Test
        @DisplayName("Retry Fallback 시 실패 사유가 저장된다")
        void processPaymentRetryFallback_ShouldSaveFailureReason() {
            // given
            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
            given(paymentRepository.save(paymentCaptor.capture())).willReturn(mockPayment);

            // when
            boolean result = paymentService.processPaymentRetryFallback(
                    mockEvent,
                    new RuntimeException("Connection timeout")
            );

            // then
            assertThat(result).isFalse();
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getPaymentStatus()).isEqualTo("FAILED");
            assertThat(savedPayment.getFailureReason()).contains("Connection timeout");
        }
    }
}
