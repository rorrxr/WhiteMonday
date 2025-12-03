package com.minju.paymentservice.service;

import com.minju.common.kafka.payment.PaymentRequestedEvent;
import com.minju.paymentservice.dto.PaymentRequestDto;
import com.minju.paymentservice.entity.Payment;
import com.minju.paymentservice.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private static final String EXTERNAL_PAYMENT_CB = "external-payment-gateway";
    private final Random random = new Random();

    /**
     * 기존 결제 진입 검증 로직 (동기)
     */
    public boolean validatePaymentEntry(PaymentRequestDto paymentRequestDto) {
        return "결제 진행 중".equals(paymentRequestDto.getPaymentStatus());
    }


    /**
     * SAGA 패턴용 결제 처리 로직 (Circuit Breaker + Retry)
     * Payment Service는 독립적으로 동작하므로 외부 의존성 없음
     */
    @CircuitBreaker(name = EXTERNAL_PAYMENT_CB, fallbackMethod = "processPaymentFallback")
    @Retry(name = EXTERNAL_PAYMENT_CB, fallbackMethod = "processPaymentRetryFallback")
    @Transactional
    public boolean processPayment(PaymentRequestedEvent event) {
        log.info("SAGA 결제 처리 시작: orderId={}, amount={}", event.getOrderId(), event.getAmount());

        try {
            // Payment 엔티티 생성 및 저장
            Payment payment = new Payment();
            payment.setOrderId(Long.parseLong(event.getOrderId()));
            payment.setUserId(Long.parseLong(event.getUserId()));
            payment.setAmount(event.getAmount());
            payment.setPaymentStatus("PROCESSING");
            payment.setPaymentMethod("CARD");

            Payment savedPayment = paymentRepository.save(payment);
            log.info("Payment 엔티티 저장 완료 - paymentId: {}", savedPayment.getId());

            // 2. 외부 결제 게이트웨이 호출 시뮬레이션
            boolean result = callExternalPaymentGateway(event);

            // 3. 결제 결과에 따른 상태 업데이트
            savedPayment.setPaymentStatus(result ? "COMPLETED" : "FAILED");
            paymentRepository.save(savedPayment);

            log.info("결제 처리 완료 - orderId: {}, result: {}", event.getOrderId(), result);
            return result;

        } catch (Exception e) {
            log.error("SAGA 결제 처리 중 오류: ", e);
            throw e;
        }
    }

    /**
     * 외부 결제 게이트웨이 호출 시뮬레이션 (80% 성공률)
     */
    private boolean callExternalPaymentGateway(PaymentRequestedEvent event) {
        // 실제로는 PG사 API 호출 (토스페이먼츠, 카카오페이 등)
        try {
            Thread.sleep(500); // 외부 API 호출 시뮬레이션

            if (random.nextInt(100) < 80) {
                log.info("외부 PG 결제 승인 - orderId: {}, amount: {}",
                        event.getOrderId(), event.getAmount());
                return true;
            } else {
                log.warn("외부 PG 결제 거부 - orderId: {}", event.getOrderId());
                throw new RuntimeException("외부 결제 게이트웨이 승인 거부");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("결제 처리 중 인터럽트 발생", e);
        }
    }

    // ==================== Circuit Breaker Fallback Methods ====================

    /**
     * 결제 Circuit Breaker Fallback
     */
    public boolean processPaymentFallback(PaymentRequestedEvent event, Exception ex) {
        log.error("결제 Circuit Breaker 활성화 - orderId: {}, error: {}",
                event.getOrderId(), ex.getMessage());

        // Circuit Breaker 활성화 시 DB에 FAILED 상태 저장
        saveFailedPayment(event, "Circuit Breaker 활성화");
        return false;
    }

    /**
     * 결제 Retry Fallback
     */
    public boolean processPaymentRetryFallback(PaymentRequestedEvent event, Exception ex) {
        log.error("결제 재시도 실패 - orderId: {}, error: {}",
                event.getOrderId(), ex.getMessage());

        saveFailedPayment(event, "재시도 실패: " + ex.getMessage());
        return false;
    }

    /**
     * 실패한 결제 정보 저장
     */
    @Transactional
    private void saveFailedPayment(PaymentRequestedEvent event, String reason) {
        try {
            Payment payment = new Payment();
            payment.setOrderId(Long.parseLong(event.getOrderId()));
            payment.setUserId(Long.parseLong(event.getUserId()));
            payment.setAmount(event.getAmount());
            payment.setPaymentStatus("FAILED");
            payment.setPaymentMethod("CARD");
            payment.setFailureReason(reason);

            paymentRepository.save(payment);
            log.info("실패한 결제 정보 저장 완료 - orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("실패한 결제 정보 저장 실패: ", e);
        }
    }
}