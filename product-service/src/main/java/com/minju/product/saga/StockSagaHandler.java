package com.minju.product.saga;

import com.minju.common.kafka.StockReservationFailedEvent;
import com.minju.common.kafka.StockReservationRequestEvent;
import com.minju.common.kafka.StockReservedEvent;
import com.minju.common.kafka.StockRestoreEvent;
import com.minju.product.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockSagaHandler {

    private final StockService stockService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "stock-reservation-requested-topic", groupId = "stock-saga-group")
    @Transactional
    public void handleStockReservationRequest(StockReservationRequestEvent event) {
        log.info("재고 예약 요청 수신: {}", event);

        try {
            Long productId = Long.parseLong(event.getProductId());
            int currentStock = stockService.getAccurateStock(productId);

            if (currentStock >= event.getQuantity()) {
                boolean success = stockService.decreaseStockWithTransaction(productId, event.getQuantity());

                if (success) {
                    StockReservedEvent successEvent = StockReservedEvent.builder()
                            .orderId(event.getOrderId())
                            .productId(event.getProductId())
                            .quantity(event.getQuantity())
                            .status("STOCK_RESERVED")
                            .build();

                    kafkaTemplate.send("stock-reserved-topic", successEvent);
                    log.info("재고 예약 성공 이벤트 발행: {}", successEvent);
                } else {
                    publishStockReservationFailedEvent(event, "재고 차감 실패");
                }

            } else {
                publishStockReservationFailedEvent(event,
                        "재고 부족 (현재: " + currentStock + ", 요청: " + event.getQuantity() + ")");
            }

        } catch (Exception e) {
            log.error("재고 예약 처리 중 오류: ", e);
            publishStockReservationFailedEvent(event, "재고 처리 오류: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "stock-restore-topic", groupId = "stock-saga-group")
    @Transactional
    public void handleStockRestore(StockRestoreEvent event) {
        log.info("재고 복구 요청 수신: {}", event);

        try {
            Long productId = Long.parseLong(event.getProductId());
            stockService.restoreStock(productId, event.getQuantity());
            log.info("재고 복구 완료 - productId: {}, quantity: {}", productId, event.getQuantity());

        } catch (Exception e) {
            log.error("재고 복구 실패: ", e);
        }
    }

    private void publishStockReservationFailedEvent(StockReservationRequestEvent originalEvent, String reason) {
        try {
            StockReservationFailedEvent failEvent = StockReservationFailedEvent.builder()
                    .orderId(originalEvent.getOrderId())
                    .productId(originalEvent.getProductId())
                    .quantity(originalEvent.getQuantity())
                    .reason(reason)
                    .status("STOCK_RESERVATION_FAILED")
                    .build();

            kafkaTemplate.send("stock-reservation-failed-topic", failEvent);
            log.error("재고 예약 실패 이벤트 발행: {}", failEvent);

        } catch (Exception e) {
            log.error("재고 예약 실패 이벤트 발행 중 오류: ", e);
        }
    }
}