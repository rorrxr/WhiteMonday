package com.minju.paymentservice.service;


import com.minju.common.kafka.PaymentCompletedEvent;
import com.minju.common.kafka.PaymentManualProcessingEvent;
import com.minju.common.kafka.PaymentRequestedEvent;
import com.minju.paymentservice.client.ProductServiceFeignClient;
import com.minju.paymentservice.dto.PaymentRequestDto;
import com.minju.paymentservice.entity.Payment;
import com.minju.paymentservice.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ProductServiceFeignClient productServiceFeignClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PRODUCT_CB = "productService";
    private static final String EXTERNAL_PAYMENT_CB = "external-payment-gateway";
    private final Random random = new Random();

    // 기존 결제 진입 검증 로직
    public boolean validatePaymentEntry(PaymentRequestDto paymentRequestDto) {
        return "결제 진행 중".equals(paymentRequestDto.getPaymentStatus());
    }

    // 기존 결제 처리 로직 (PaymentRequestDto용)
    @CircuitBreaker(name = PRODUCT_CB, fallbackMethod = "restoreStockFallback")
    public boolean processPayment(PaymentRequestDto requestDto) {
        try {
            // 20% 실패 시뮬레이션
            boolean isSuccess = new Random().nextInt(100) >= 20;

            if (isSuccess) {
                // 결제 성공 처리
                log.info("Payment succeeded for product ID: {}", requestDto.getProductId());
                return true;
            } else {
                // 결제 실패 처리: 재고 복구 요청
                log.info("Payment failed for product ID: {}. Restoring stock...", requestDto.getProductId());
                productServiceFeignClient.restoreStock(requestDto.getProductId(), requestDto.getQuantity());
                return false;
            }
        } catch (Exception e) {
            log.error("Error occurred during payment processing: ", e);
            throw new RuntimeException("결제 처리 중 오류 발생", e);
        }
    }

    // SAGA 패턴용 결제 처리 로직 (PaymentRequestedEvent용)
    @CircuitBreaker(name = EXTERNAL_PAYMENT_CB, fallbackMethod = "processPaymentFallback")
    @Retry(name = EXTERNAL_PAYMENT_CB, fallbackMethod = "processPaymentRetryFallback")
    @Transactional
    public boolean processPayment(PaymentRequestedEvent event) {
        log.info("SAGA 결제 처리 시작: {}", event);

        try {
            // 결제 정보 저장
            Payment payment = new Payment();
            payment.setPaymentStatus("PROCESSING");
            payment.setPaymentMethod("CARD");
            paymentRepository.save(payment);

            // 외부 결제 게이트웨이 호출 시뮬레이션
            boolean result = callExternalPaymentGateway(event);

            // 결제 결과에 따른 상태 업데이트
            payment.setPaymentStatus(result ? "COMPLETED" : "FAILED");
            paymentRepository.save(payment);

            return result;

        } catch (Exception e) {
            log.error("SAGA 결제 처리 중 오류: ", e);
            throw e;
        }
    }

    private boolean callExternalPaymentGateway(PaymentRequestedEvent event) {
        // 외부 결제 시스템 호출 시뮬레이션 (80% 성공률)
        if (random.nextInt(100) < 80) {
            log.info("결제 성공 - orderId: {}, amount: {}", event.getOrderId(), event.getAmount());
            return true;
        } else {
            log.warn("결제 실패 - orderId: {}", event.getOrderId());
            throw new RuntimeException("외부 결제 게이트웨이 오류");
        }
    }

    // Fallback Methods for SAGA pattern
    public boolean processPaymentFallback(PaymentRequestedEvent event, Exception ex) {
        log.error("결제 Circuit Breaker 활성화 - orderId: {}, error: {}",
                event.getOrderId(), ex.getMessage());

        // 수동 처리 이벤트 발행
        publishPaymentManualProcessingEvent(event);
        return false;
    }

    public boolean processPaymentRetryFallback(PaymentRequestedEvent event, Exception ex) {
        log.error("결제 재시도 실패 - orderId: {}, error: {}",
                event.getOrderId(), ex.getMessage());

        publishPaymentManualProcessingEvent(event);
        return false;
    }

    // Fallback Method for original pattern
    public boolean restoreStockFallback(PaymentRequestDto requestDto, Throwable t) {
        log.error("재고 복구 실패 (fallback). Error: {}", t.getMessage());
        return false;
    }

    private void publishPaymentManualProcessingEvent(PaymentRequestedEvent event) {
        try {
            PaymentManualProcessingEvent manualEvent = PaymentManualProcessingEvent.builder()
                    .orderId(event.getOrderId())
                    .amount(event.getAmount())
                    .reason("자동 결제 실패 - 수동 처리 필요")
                    .status("MANUAL_PROCESSING_REQUIRED")
                    .build();

            kafkaTemplate.send("payment-manual-processing-topic", manualEvent);
            log.info("수동 처리 이벤트 발행: {}", manualEvent);

        } catch (Exception e) {
            log.error("수동 처리 이벤트 발행 실패: ", e);
        }
    }
}