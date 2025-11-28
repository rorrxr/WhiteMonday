package com.minju.product.saga;

import com.minju.common.kafka.stock.StockReservationFailedEvent;
import com.minju.common.kafka.StockReservationRequestEvent;
import com.minju.common.kafka.StockReservedEvent;
import com.minju.common.kafka.stock.StockRestoreEvent;
import com.minju.product.outbox.OutboxEventPublisher;
import com.minju.product.service.StockService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockSagaHandler {

    private final StockService stockService;
    private final OutboxEventPublisher outboxPublisher;

    private static final String REDIS_CB = "redis-operation";

    /**
     * 재고 예약 요청 처리 - Outbox 패턴 적용
     * Circuit Breaker로 Redis/DB 장애 대응
     */
    @KafkaListener(topics = "stock-reservation-requested-topic", groupId = "stock-saga-group")
    @CircuitBreaker(name = REDIS_CB, fallbackMethod = "handleStockReservationFallback")
    @Retry(name = REDIS_CB)
    @Transactional
    public void handleStockReservationRequest(StockReservationRequestEvent event) {
        log.info("재고 예약 요청 수신: orderId={}, productId={}, quantity={}",
                event.getOrderId(), event.getProductId(), event.getQuantity());

        try {
            Long productId = Long.parseLong(event.getProductId());

            // 현재 재고 확인
            int currentStock = stockService.getAccurateStock(productId);
            log.info("현재 재고: productId={}, stock={}", productId, currentStock);

            if (currentStock >= event.getQuantity()) {
                // 재고 차감 시도
                boolean success = stockService.decreaseStockWithTransaction(productId, event.getQuantity());

                if (success) {
                    // 재고 차감 성공 시, Outbox에 성공 이벤트 저장
                    StockReservedEvent successEvent = StockReservedEvent.builder()
                            .orderId(event.getOrderId())
                            .productId(event.getProductId())
                            .quantity(event.getQuantity())
                            .status("STOCK_RESERVED")
                            .build();

                    outboxPublisher.saveEvent(
                            "STOCK",
                            event.getProductId(),
                            "STOCK_RESERVED",
                            "stock-reserved-topic",
                            successEvent
                    );
                    log.info("재고 예약 성공 및 Outbox 저장 - orderId: {}", event.getOrderId());

                } else {
                    // 재고 차감 실패
                    publishStockReservationFailedEvent(event, "재고 차감 트랜잭션 실패");
                }

            } else {
                // 재고 부족
                publishStockReservationFailedEvent(event,
                        "재고 부족 (현재: " + currentStock + ", 요청: " + event.getQuantity() + ")");
            }

        } catch (Exception e) {
            log.error("재고 예약 처리 중 오류: orderId={}", event.getOrderId(), e);
            publishStockReservationFailedEvent(event, "재고 처리 오류: " + e.getMessage());
        }
    }

    /**
     * 재고 복구 요청 처리 - Outbox 패턴 적용
     */
    @KafkaListener(topics = "stock-restore-topic", groupId = "stock-saga-group")
    @CircuitBreaker(name = REDIS_CB, fallbackMethod = "handleStockRestoreFallback")
    @Retry(name = REDIS_CB)
    @Transactional
    public void handleStockRestore(StockRestoreEvent event) {
        log.info("재고 복구 요청 수신: orderId={}, productId={}, quantity={}",
                event.getOrderId(), event.getProductId(), event.getQuantity());

        try {
            Long productId = Long.parseLong(event.getProductId());

            // 재고 복구
            stockService.restoreStock(productId, event.getQuantity());

            log.info("재고 복구 완료 - productId: {}, quantity: {}",
                    productId, event.getQuantity());

        } catch (Exception e) {
            log.error("재고 복구 실패: orderId={}", event.getOrderId(), e);
            // 재고 복구 실패는 치명적 - 별도 모니터링/알림 필요
            // DLQ(Dead Letter Queue)로 전송하거나 수동 처리 필요
        }
    }

    // ==================== Circuit Breaker Fallback Methods ====================

    /**
     * 재고 예약 Fallback
     */
    public void handleStockReservationFallback(StockReservationRequestEvent event, Exception ex) {
        log.error("재고 예약 Circuit Breaker 활성화 - orderId: {}, error: {}",
                event.getOrderId(), ex.getMessage());
        publishStockReservationFailedEvent(event, "Circuit Breaker 활성화");
    }

    /**
     * 재고 복구 Fallback
     */
    public void handleStockRestoreFallback(StockRestoreEvent event, Exception ex) {
        log.error("재고 복구 Circuit Breaker 활성화 - orderId: {}, error: {}",
                event.getOrderId(), ex.getMessage());
        // 재고 복구 실패는 치명적이므로 별도 처리 필요
    }

    /**
     * 재고 예약 실패 이벤트 발행 (Outbox)
     */
    private void publishStockReservationFailedEvent(StockReservationRequestEvent originalEvent,
                                                    String reason) {
        try {
            StockReservationFailedEvent failEvent = StockReservationFailedEvent.builder()
                    .orderId(originalEvent.getOrderId())
                    .productId(originalEvent.getProductId())
                    .quantity(originalEvent.getQuantity())
                    .reason(reason)
                    .status("STOCK_RESERVATION_FAILED")
                    .build();

            outboxPublisher.saveEvent(
                    "STOCK",
                    originalEvent.getProductId(),
                    "STOCK_RESERVATION_FAILED",
                    "stock-reservation-failed-topic",
                    failEvent
            );
            log.error("재고 예약 실패 Outbox 저장: orderId={}, reason={}",
                    originalEvent.getOrderId(), reason);

        } catch (Exception e) {
            log.error("재고 예약 실패 이벤트 저장 중 오류: ", e);
        }
    }
}