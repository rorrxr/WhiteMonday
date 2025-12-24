package com.minju.product.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minju.common.dlq.DeadLetterEvent;
import com.minju.common.dlq.DeadLetterEventRepository;
import com.minju.common.idempotency.ProcessedEventRepository;
import com.minju.common.kafka.stock.StockReservationRequestEvent;
import com.minju.common.kafka.stock.StockRestoreEvent;
import com.minju.product.outbox.OutboxEventPublisher;
import com.minju.product.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockSagaHandler 단위 테스트")
class StockSagaHandlerTest {

    @Mock
    private StockService stockService;

    @Mock
    private OutboxEventPublisher outboxPublisher;

    @Mock
    private DeadLetterEventRepository deadLetterEventRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private StockSagaHandler stockSagaHandler;

    @Nested
    @DisplayName("재고 예약 요청 처리")
    class HandleStockReservationRequestTest {

        @Test
        @DisplayName("재고가 충분하면 STOCK_RESERVED 이벤트가 발행된다")
        void handleStockReservationRequest_SufficientStock_ShouldPublishReserved() {
            // given
            StockReservationRequestEvent event = StockReservationRequestEvent.builder()
                    .orderId("1")
                    .productId("1")
                    .quantity(5)
                    .status("STOCK_RESERVATION_REQUESTED")
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(stockService.getAccurateStock(1L)).willReturn(100);
            given(stockService.decreaseStockWithTransaction(1L, 5)).willReturn(true);

            // when
            stockSagaHandler.handleStockReservationRequest(event);

            // then
            verify(outboxPublisher).saveEvent(
                    eq("STOCK"),
                    eq("1"),
                    eq("STOCK_RESERVED"),
                    eq("stock-reserved-topic"),
                    any()
            );
        }

        @Test
        @DisplayName("재고가 부족하면 STOCK_RESERVATION_FAILED 이벤트가 발행된다")
        void handleStockReservationRequest_InsufficientStock_ShouldPublishFailed() {
            // given
            StockReservationRequestEvent event = StockReservationRequestEvent.builder()
                    .orderId("1")
                    .productId("1")
                    .quantity(100)
                    .status("STOCK_RESERVATION_REQUESTED")
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(stockService.getAccurateStock(1L)).willReturn(10);

            // when
            stockSagaHandler.handleStockReservationRequest(event);

            // then
            verify(outboxPublisher).saveEvent(
                    eq("STOCK"),
                    eq("1"),
                    eq("STOCK_RESERVATION_FAILED"),
                    eq("stock-reservation-failed-topic"),
                    any()
            );
        }

        @Test
        @DisplayName("재고 차감 트랜잭션 실패 시 STOCK_RESERVATION_FAILED 이벤트가 발행된다")
        void handleStockReservationRequest_TransactionFailed_ShouldPublishFailed() {
            // given
            StockReservationRequestEvent event = StockReservationRequestEvent.builder()
                    .orderId("1")
                    .productId("1")
                    .quantity(5)
                    .status("STOCK_RESERVATION_REQUESTED")
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(stockService.getAccurateStock(1L)).willReturn(100);
            given(stockService.decreaseStockWithTransaction(1L, 5)).willReturn(false);

            // when
            stockSagaHandler.handleStockReservationRequest(event);

            // then
            verify(outboxPublisher).saveEvent(
                    eq("STOCK"),
                    eq("1"),
                    eq("STOCK_RESERVATION_FAILED"),
                    eq("stock-reservation-failed-topic"),
                    any()
            );
        }

        @Test
        @DisplayName("중복 이벤트는 무시된다 (멱등성)")
        void handleStockReservationRequest_DuplicateEvent_ShouldBeIgnored() {
            // given
            StockReservationRequestEvent event = StockReservationRequestEvent.builder()
                    .orderId("1")
                    .productId("1")
                    .quantity(5)
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(true);

            // when
            stockSagaHandler.handleStockReservationRequest(event);

            // then
            verify(stockService, never()).getAccurateStock(anyLong());
            verify(outboxPublisher, never()).saveEvent(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("재고 복구 처리")
    class HandleStockRestoreTest {

        @Test
        @DisplayName("재고 복구 요청이 성공하면 재고가 복구된다")
        void handleStockRestore_ShouldRestoreStock() {
            // given
            StockRestoreEvent event = StockRestoreEvent.builder()
                    .orderId("1")
                    .productId("1")
                    .quantity(5)
                    .reason("결제 실패")
                    .status("STOCK_RESTORE_REQUESTED")
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(false);

            // when
            stockSagaHandler.handleStockRestore(event);

            // then
            verify(stockService).restoreStock(1L, 5);
        }

        @Test
        @DisplayName("재고 복구 실패 시 DLQ에 저장된다")
        void handleStockRestore_Failed_ShouldSaveToDLQ() {
            // given
            StockRestoreEvent event = StockRestoreEvent.builder()
                    .orderId("1")
                    .productId("1")
                    .quantity(5)
                    .reason("결제 실패")
                    .status("STOCK_RESTORE_REQUESTED")
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(false);
            willThrow(new RuntimeException("DB 연결 실패"))
                    .given(stockService).restoreStock(1L, 5);

            // when
            stockSagaHandler.handleStockRestore(event);

            // then
            verify(deadLetterEventRepository).save(any(DeadLetterEvent.class));
        }

        @Test
        @DisplayName("중복 복구 이벤트는 무시된다 (멱등성)")
        void handleStockRestore_DuplicateEvent_ShouldBeIgnored() {
            // given
            StockRestoreEvent event = StockRestoreEvent.builder()
                    .orderId("1")
                    .productId("1")
                    .quantity(5)
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(true);

            // when
            stockSagaHandler.handleStockRestore(event);

            // then
            verify(stockService, never()).restoreStock(anyLong(), anyInt());
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Fallback")
    class CircuitBreakerFallbackTest {

        @Test
        @DisplayName("재고 예약 Fallback 시 STOCK_RESERVATION_FAILED가 발행된다")
        void handleStockReservationFallback_ShouldPublishFailed() {
            // given
            StockReservationRequestEvent event = StockReservationRequestEvent.builder()
                    .orderId("1")
                    .productId("1")
                    .quantity(5)
                    .build();

            // when
            stockSagaHandler.handleStockReservationFallback(event, new RuntimeException("Circuit Breaker Open"));

            // then
            verify(outboxPublisher).saveEvent(
                    eq("STOCK"),
                    eq("1"),
                    eq("STOCK_RESERVATION_FAILED"),
                    eq("stock-reservation-failed-topic"),
                    any()
            );
        }

        @Test
        @DisplayName("재고 복구 Fallback 시 DLQ에 저장된다")
        void handleStockRestoreFallback_ShouldSaveToDLQ() {
            // given
            StockRestoreEvent event = StockRestoreEvent.builder()
                    .orderId("1")
                    .productId("1")
                    .quantity(5)
                    .reason("결제 실패")
                    .build();

            // when
            stockSagaHandler.handleStockRestoreFallback(event, new RuntimeException("Circuit Breaker Open"));

            // then
            verify(deadLetterEventRepository).save(any(DeadLetterEvent.class));
        }
    }
}
