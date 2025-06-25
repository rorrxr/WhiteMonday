package com.minju.paymentservice.service;

import com.minju.common.kafka.OrderCreatedEvent;
import com.minju.paymentservice.client.ProductServiceFeignClient;
import com.minju.paymentservice.dto.PaymentRequestDto;
import com.minju.paymentservice.entity.Payment;
import com.minju.paymentservice.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {


    private final PaymentRepository paymentRepository;
    private final ProductServiceFeignClient productServiceFeignClient;
    private static final String PRODUCT_CB = "productService";

    // 결제 진입 검증 로직
    public boolean validatePaymentEntry(PaymentRequestDto paymentRequestDto) {
        return "결제 진행 중".equals(paymentRequestDto.getPaymentStatus());
    }


    @KafkaListener(topics = "order-created-topic", groupId = "payment-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Kafka - 주문 생성 이벤트 수신: {}", event);

        Payment payment = new Payment();
        payment.setOrderId((int) Long.parseLong(String.valueOf(event.getOrderId())));
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentMethod("신용카드"); // 예시

        boolean isSuccess = new Random().nextInt(100) >= 20;
        payment.setPaymentStatus(isSuccess ? "성공" : "실패");

        paymentRepository.save(payment);
        log.info("결제 {} - 주문 ID: {}", isSuccess ? "성공" : "실패", event.getOrderId());

        // Kafka를 통해 재고 차감 메시지 추가
    }

    // 결제 처리 로직
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

    public boolean restoreStockFallback(PaymentRequestDto requestDto, Throwable t) {
        log.error("재고 복구 실패 (fallback). Error: {}", t.getMessage());
        // 재고 복구 실패시 DB에 실패 기록 남기거나 알림을 전송할 수도 있음
        return false; // 결제 실패 처리 유지
    }
}
