package com.minju.order.saga;

import com.minju.common.kafka.*;
import com.minju.order.entity.Orders;
import com.minju.order.outbox.OutboxEventPublisher;
import com.minju.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator {

    private final OrderRepository orderRepository;
    private final OutboxEventPublisher outboxPublisher;

    /**
     * 재고 예약 성공 이벤트 수신
     * → 결제 요청 이벤트 발행 (Outbox)
     */
    @KafkaListener(topics = "stock-reserved-topic", groupId = "order-saga-group")
    @Transactional
    public void handleStockReserved(StockReservedEvent event) {
        log.info("재고 예약 성공 수신: orderId={}, productId={}",
                event.getOrderId(), event.getProductId());

        try {
            // 주문 상태 업데이트
            Orders order = orderRepository.findById(Long.parseLong(event.getOrderId()))
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + event.getOrderId()));

            order.setOrderStatus("STOCK_RESERVED");
            orderRepository.save(order);
            log.info("주문 상태 업데이트: PENDING → STOCK_RESERVED");

            // Outbox를 통한 결제 요청 이벤트 발행
            PaymentRequestedEvent paymentEvent = PaymentRequestedEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(String.valueOf(order.getUserId()))
                    .productId(event.getProductId())
                    .quantity(event.getQuantity())
                    .amount(order.getTotalAmount())
                    .status("PAYMENT_REQUESTED")
                    .build();

            outboxPublisher.saveEvent(
                    "ORDER",
                    event.getOrderId(),
                    "PAYMENT_REQUESTED",
                    "payment-requested-topic",
                    paymentEvent
            );
            log.info("결제 요청 Outbox 저장 완료 - orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("재고 예약 성공 처리 중 오류: orderId={}", event.getOrderId(), e);
            publishStockRestoreEvent(event, "SAGA 처리 오류");
        }
    }

    /**
     * 재고 예약 실패 이벤트 수신
     * → 주문 취소
     */
    @KafkaListener(topics = "stock-reservation-failed-topic", groupId = "order-saga-group")
    @Transactional
    public void handleStockReservationFailed(StockReservationFailedEvent event) {
        log.error("재고 예약 실패 수신: orderId={}, reason={}",
                event.getOrderId(), event.getReason());

        try {
            Orders order = orderRepository.findById(Long.parseLong(event.getOrderId()))
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + event.getOrderId()));

            order.setOrderStatus("CANCELLED");
            orderRepository.save(order);
            log.info("주문 상태 업데이트: PENDING → CANCELLED (재고 부족)");

            // Outbox를 통한 주문 취소 이벤트 발행
            OrderCancelledEvent cancelEvent = OrderCancelledEvent.builder()
                    .orderId(event.getOrderId())
                    .reason("재고 부족: " + event.getReason())
                    .status("CANCELLED")
                    .build();

            outboxPublisher.saveEvent(
                    "ORDER",
                    event.getOrderId(),
                    "ORDER_CANCELLED",
                    "order-cancelled-topic",
                    cancelEvent
            );
            log.info("주문 취소 Outbox 저장 완료 - orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("재고 예약 실패 처리 중 오류: orderId={}", event.getOrderId(), e);
        }
    }

    /**
     * 결제 완료 이벤트 수신
     * → 주문 완료 or 실패 처리
     */
    @KafkaListener(topics = "payment-completed-topic", groupId = "order-saga-group")
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 수신: orderId={}, success={}",
                event.getOrderId(), event.isSuccess());

        try {
            Orders order = orderRepository.findById(Long.parseLong(event.getOrderId()))
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + event.getOrderId()));

            if (event.isSuccess()) {
                // 결제 성공 → 주문 완료
                order.setOrderStatus("COMPLETED");
                orderRepository.save(order);
                log.info("주문 상태 업데이트: STOCK_RESERVED → COMPLETED");

                // Outbox를 통한 주문 완료 이벤트 발행
                OrderCompletedEvent completedEvent = OrderCompletedEvent.builder()
                        .orderId(event.getOrderId())
                        .status("COMPLETED")
                        .build();

                outboxPublisher.saveEvent(
                        "ORDER",
                        event.getOrderId(),
                        "ORDER_COMPLETED",
                        "order-completed-topic",
                        completedEvent
                );
                log.info("주문 완료 Outbox 저장 완료 - orderId: {}", event.getOrderId());

            } else {
                // 결제 실패 → 보상 트랜잭션 (재고 복구)
                handlePaymentFailure(event);
            }

        } catch (Exception e) {
            log.error("결제 완료 처리 중 오류: orderId={}", event.getOrderId(), e);
            handlePaymentFailure(event);
        }
    }

    /**
     * 결제 실패 이벤트 수신
     * → 재고 복구 + 주문 취소
     */
    @KafkaListener(topics = "payment-failed-topic", groupId = "order-saga-group")
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.error("결제 실패 수신: orderId={}, reason={}",
                event.getOrderId(), event.getReason());
        handlePaymentFailure(event);
    }

    /**
     * 결제 실패 공통 처리 로직 (보상 트랜잭션)
     */
    private void handlePaymentFailure(Object event) {
        try {
            String orderId;
            String productId;
            int quantity;
            String reason;

            if (event instanceof PaymentCompletedEvent pce) {
                orderId = pce.getOrderId();
                productId = pce.getProductId();
                quantity = pce.getQuantity();
                reason = "결제 실패";
            } else if (event instanceof PaymentFailedEvent pfe) {
                orderId = pfe.getOrderId();
                productId = pfe.getProductId();
                quantity = pfe.getQuantity();
                reason = pfe.getReason();
            } else {
                log.error("알 수 없는 이벤트 타입: {}", event.getClass());
                return;
            }

            // 주문 상태 업데이트
            Orders order = orderRepository.findById(Long.parseLong(orderId))
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

            order.setOrderStatus("PAYMENT_FAILED");
            orderRepository.save(order);
            log.info("주문 상태 업데이트: STOCK_RESERVED → PAYMENT_FAILED");

            // Outbox를 통한 재고 복구 이벤트 발행 (보상 트랜잭션)
            publishStockRestoreEvent(orderId, productId, quantity, reason);

        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류: ", e);
        }
    }

    /**
     * 재고 복구 이벤트 발행 (Outbox)
     */
    private void publishStockRestoreEvent(StockReservedEvent originalEvent, String reason) {
        publishStockRestoreEvent(originalEvent.getOrderId(), originalEvent.getProductId(),
                originalEvent.getQuantity(), reason);
    }

    private void publishStockRestoreEvent(String orderId, String productId, int quantity, String reason) {
        try {
            StockRestoreEvent restoreEvent = StockRestoreEvent.builder()
                    .orderId(orderId)
                    .productId(productId)
                    .quantity(quantity)
                    .reason(reason)
                    .status("STOCK_RESTORE_REQUESTED")
                    .build();

            outboxPublisher.saveEvent(
                    "ORDER",
                    orderId,
                    "STOCK_RESTORE_REQUESTED",
                    "stock-restore-topic",
                    restoreEvent
            );
            log.info("재고 복구 Outbox 저장 완료 - orderId: {}, reason: {}", orderId, reason);

        } catch (Exception e) {
            log.error("재고 복구 이벤트 발행 실패: ", e);
        }
    }
}