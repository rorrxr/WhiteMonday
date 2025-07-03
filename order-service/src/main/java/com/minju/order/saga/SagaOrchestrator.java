package com.minju.order.saga;

import com.minju.common.kafka.*;
import com.minju.order.entity.Orders;
import com.minju.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;

    @KafkaListener(topics = "stock-reserved-topic", groupId = "order-saga-group")
    @Transactional
    public void handleStockReserved(StockReservedEvent event) {
        log.info("재고 예약 성공 수신: {}", event);

        try {
            Orders order = orderRepository.findById(Long.parseLong(event.getOrderId()))
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + event.getOrderId()));

            order.setOrderStatus("STOCK_RESERVED");
            orderRepository.save(order);

            PaymentRequestedEvent paymentEvent = PaymentRequestedEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(String.valueOf(order.getUserId()))
                    .productId(event.getProductId())
                    .quantity(event.getQuantity())
                    .amount(order.getTotalAmount())
                    .status("PAYMENT_REQUESTED")
                    .build();

            kafkaTemplate.send("payment-requested-topic", paymentEvent);
            log.info("결제 요청 이벤트 발행: {}", paymentEvent);

        } catch (Exception e) {
            log.error("재고 예약 성공 처리 중 오류: ", e);
            publishStockRestoreEvent(event, "SAGA 처리 오류");
        }
    }

    @KafkaListener(topics = "stock-reservation-failed-topic", groupId = "order-saga-group")
    @Transactional
    public void handleStockReservationFailed(StockReservationFailedEvent event) {
        log.error("재고 예약 실패 수신: {}", event);

        try {
            Orders order = orderRepository.findById(Long.parseLong(event.getOrderId()))
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + event.getOrderId()));

            order.setOrderStatus("CANCELLED");
            orderRepository.save(order);

            OrderCancelledEvent cancelEvent = OrderCancelledEvent.builder()
                    .orderId(event.getOrderId())
                    .reason("재고 부족: " + event.getReason())
                    .status("CANCELLED")
                    .build();

            kafkaTemplate.send("order-cancelled-topic", cancelEvent);
            log.info("주문 취소 이벤트 발행: {}", cancelEvent);

        } catch (Exception e) {
            log.error("재고 예약 실패 처리 중 오류: ", e);
        }
    }

    @KafkaListener(topics = "payment-completed-topic", groupId = "order-saga-group")
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 수신: {}", event);

        try {
            Orders order = orderRepository.findById(Long.parseLong(event.getOrderId()))
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + event.getOrderId()));

            if (event.isSuccess()) {
                order.setOrderStatus("COMPLETED");
                orderRepository.save(order);

                OrderCompletedEvent completedEvent = OrderCompletedEvent.builder()
                        .orderId(event.getOrderId())
                        .status("COMPLETED")
                        .build();

                kafkaTemplate.send("order-completed-topic", completedEvent);
                log.info("주문 완료 이벤트 발행: {}", completedEvent);

            } else {
                handlePaymentFailure(event);
            }

        } catch (Exception e) {
            log.error("결제 완료 처리 중 오류: ", e);
            handlePaymentFailure(event);
        }
    }

    @KafkaListener(topics = "payment-failed-topic", groupId = "order-saga-group")
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.error("결제 실패 수신: {}", event);
        handlePaymentFailure(event);
    }

    private void handlePaymentFailure(Object event) {
        try {
            String orderId;
            String productId;
            int quantity;
            String reason;

            if (event instanceof PaymentCompletedEvent) {
                PaymentCompletedEvent pce = (PaymentCompletedEvent) event;
                orderId = String.valueOf(pce.getOrderId());
                productId = String.valueOf(pce.getProductId());
                quantity = pce.getQuantity();
                reason = "결제 실패";
            } else if (event instanceof PaymentFailedEvent) {
                PaymentFailedEvent pfe = (PaymentFailedEvent) event;
                orderId = pfe.getOrderId();
                productId = pfe.getProductId();
                quantity = pfe.getQuantity();
                reason = pfe.getReason();
            } else {
                log.error("알 수 없는 이벤트 타입: {}", event.getClass());
                return;
            }

            Orders order = orderRepository.findById(Long.parseLong(orderId))
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

            order.setOrderStatus("PAYMENT_FAILED");
            orderRepository.save(order);

            publishStockRestoreEvent(orderId, productId, quantity, reason);

        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류: ", e);
        }
    }

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

            kafkaTemplate.send("stock-restore-topic", restoreEvent);
            log.info("재고 복구 이벤트 발행: {}", restoreEvent);

        } catch (Exception e) {
            log.error("재고 복구 이벤트 발행 실패: ", e);
        }
    }
}