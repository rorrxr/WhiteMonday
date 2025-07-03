package com.minju.paymentservice.saga;

import com.minju.common.kafka.PaymentCompletedEvent;
import com.minju.common.kafka.PaymentFailedEvent;
import com.minju.common.kafka.PaymentRequestedEvent;
import com.minju.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSagaHandler {

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "payment-requested-topic", groupId = "payment-saga-group")
    @Transactional
    public void handlePaymentRequest(PaymentRequestedEvent event) {
        log.info("결제 요청 수신: {}", event);

        try {
            boolean paymentSuccess = paymentService.processPayment(event);

            PaymentCompletedEvent completedEvent = PaymentCompletedEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .productId(event.getProductId())
                    .quantity(event.getQuantity())
                    .amount(event.getAmount())
                    .success(paymentSuccess)
                    .status(paymentSuccess ? "PAYMENT_COMPLETED" : "PAYMENT_FAILED")
                    .build();

            kafkaTemplate.send("payment-completed-topic", completedEvent);
            log.info("결제 완료 이벤트 발행: {}", completedEvent);

        } catch (Exception e) {
            log.error("결제 처리 중 오류: ", e);

            PaymentFailedEvent failEvent = PaymentFailedEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .productId(event.getProductId())
                    .quantity(event.getQuantity())
                    .reason("결제 처리 오류: " + e.getMessage())
                    .status("PAYMENT_FAILED")
                    .build();

            kafkaTemplate.send("payment-failed-topic", failEvent);
            log.error("결제 실패 이벤트 발행: {}", failEvent);
        }
    }
}