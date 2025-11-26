package com.minju.paymentservice.saga;

import com.minju.common.kafka.PaymentCompletedEvent;
import com.minju.common.kafka.PaymentFailedEvent;
import com.minju.common.kafka.PaymentRequestedEvent;
import com.minju.paymentservice.outbox.OutboxEventPublisher;
import com.minju.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSagaHandler {

    private final PaymentService paymentService;
    private final OutboxEventPublisher outboxPublisher;

    /**
     * 결제 요청 처리 - Outbox 패턴 적용
     * 이벤트 소비 → DB 저장 → Outbox 저장이 하나의 트랜잭션
     */
    @KafkaListener(topics = "payment-requested-topic", groupId = "payment-saga-group")
    @Transactional
    public void handlePaymentRequest(PaymentRequestedEvent event) {
        log.info("결제 요청 수신: orderId={}, amount={}", event.getOrderId(), event.getAmount());

        try {
            // 1. 결제 처리 (Payment 엔티티 저장 포함)
            boolean paymentSuccess = paymentService.processPayment(event);

            if (paymentSuccess) {
                // 2-1. 결제 성공 시 Outbox에 성공 이벤트 저장
                PaymentCompletedEvent completedEvent = PaymentCompletedEvent.builder()
                        .orderId(event.getOrderId())
                        .userId(event.getUserId())
                        .productId(event.getProductId())
                        .quantity(event.getQuantity())
                        .amount(event.getAmount())
                        .success(true)
                        .status("PAYMENT_COMPLETED")
                        .build();

                outboxPublisher.saveEvent(
                        "PAYMENT",
                        event.getOrderId(),
                        "PAYMENT_COMPLETED",
                        "payment-completed-topic",
                        completedEvent
                );
                log.info("결제 완료 Outbox 저장 성공 - orderId: {}", event.getOrderId());

            } else {
                // 2-2. 결제 실패 → Outbox에 실패 이벤트 저장
                publishPaymentFailedEvent(event, "결제 처리 실패");
            }

        } catch (Exception e) {
            log.error("결제 처리 중 오류: orderId={}", event.getOrderId(), e);
            publishPaymentFailedEvent(event, "결제 처리 오류: " + e.getMessage());
        }
    }

    /**
     * 결제 실패 이벤트 발행 (Outbox)
     */
    private void publishPaymentFailedEvent(PaymentRequestedEvent event, String reason) {
        try {
            PaymentFailedEvent failEvent = PaymentFailedEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .productId(event.getProductId())
                    .quantity(event.getQuantity())
                    .reason(reason)
                    .status("PAYMENT_FAILED")
                    .build();

            outboxPublisher.saveEvent(
                    "PAYMENT",
                    event.getOrderId(),
                    "PAYMENT_FAILED",
                    "payment-failed-topic",
                    failEvent
            );
            log.error("결제 실패 Outbox 저장: orderId={}, reason={}",
                    event.getOrderId(), reason);

        } catch (Exception e) {
            log.error("결제 실패 이벤트 저장 중 오류: ", e);
        }
    }
}