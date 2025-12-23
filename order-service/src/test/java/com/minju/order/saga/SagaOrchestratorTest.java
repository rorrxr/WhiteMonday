package com.minju.order.saga;

import com.minju.common.idempotency.ProcessedEvent;
import com.minju.common.idempotency.ProcessedEventRepository;
import com.minju.common.kafka.payment.PaymentCompletedEvent;
import com.minju.common.kafka.payment.PaymentFailedEvent;
import com.minju.common.kafka.stock.StockReservationFailedEvent;
import com.minju.common.kafka.stock.StockReservedEvent;
import com.minju.order.entity.OrderItem;
import com.minju.order.entity.Orders;
import com.minju.order.outbox.OutboxEventPublisher;
import com.minju.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SagaOrchestrator 단위 테스트")
class SagaOrchestratorTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventPublisher outboxPublisher;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private SagaOrchestrator sagaOrchestrator;

    private Orders mockOrder;

    @BeforeEach
    void setUp() {
        mockOrder = new Orders();
        mockOrder.setId(1L);
        mockOrder.setUserId(1L);
        mockOrder.setOrderStatus("PENDING");
        mockOrder.setTotalAmount(35000);
        mockOrder.setTotalItemCount(2);
        mockOrder.setReservedItemCount(0);
        mockOrder.setFailedItemCount(0);

        // 주문 항목 추가
        OrderItem item1 = new OrderItem();
        item1.setProductId(1L);
        item1.setQuantity(2);
        item1.setOrder(mockOrder);
        mockOrder.getOrderItems().add(item1);

        OrderItem item2 = new OrderItem();
        item2.setProductId(2L);
        item2.setQuantity(1);
        item2.setOrder(mockOrder);
        mockOrder.getOrderItems().add(item2);
    }

    @Nested
    @DisplayName("재고 예약 성공 처리")
    class HandleStockReservedTest {

        @Test
        @DisplayName("첫 번째 상품 재고 예약 성공 시 카운터가 증가한다")
        void handleStockReserved_FirstProduct_ShouldIncrementCounter() {
            // given
            StockReservedEvent event = StockReservedEvent.builder()
                    .orderId("1")
                    .productId("1")
                    .quantity(2)
                    .status("STOCK_RESERVED")
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(orderRepository.findById(1L)).willReturn(Optional.of(mockOrder));
            given(orderRepository.save(any(Orders.class))).willReturn(mockOrder);

            // when
            sagaOrchestrator.handleStockReserved(event);

            // then
            assertThat(mockOrder.getReservedItemCount()).isEqualTo(1);
            verify(outboxPublisher, never()).saveEvent(
                    anyString(), anyString(), eq("PAYMENT_REQUESTED"), anyString(), any()
            );
        }

        @Test
        @DisplayName("모든 상품 재고 예약 완료 시 결제 요청 이벤트가 발행된다")
        void handleStockReserved_AllReserved_ShouldPublishPaymentRequest() {
            // given
            mockOrder.setReservedItemCount(1); // 이미 1개 예약됨
            mockOrder.setTotalItemCount(2);

            StockReservedEvent event = StockReservedEvent.builder()
                    .orderId("1")
                    .productId("2")
                    .quantity(1)
                    .status("STOCK_RESERVED")
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(orderRepository.findById(1L)).willReturn(Optional.of(mockOrder));
            given(orderRepository.save(any(Orders.class))).willReturn(mockOrder);

            // when
            sagaOrchestrator.handleStockReserved(event);

            // then
            assertThat(mockOrder.getOrderStatus()).isEqualTo("STOCK_RESERVED");
            verify(outboxPublisher).saveEvent(
                    eq("ORDER"),
                    eq("1"),
                    eq("PAYMENT_REQUESTED"),
                    eq("payment-requested-topic"),
                    any()
            );
        }

        @Test
        @DisplayName("이미 실패한 상품이 있으면 재고 복구 이벤트가 발행된다")
        void handleStockReserved_HasFailedItem_ShouldPublishStockRestore() {
            // given
            mockOrder.setFailedItemCount(1); // 이미 실패한 상품 있음

            StockReservedEvent event = StockReservedEvent.builder()
                    .orderId("1")
                    .productId("1")
                    .quantity(2)
                    .status("STOCK_RESERVED")
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(orderRepository.findById(1L)).willReturn(Optional.of(mockOrder));
            given(orderRepository.save(any(Orders.class))).willReturn(mockOrder);

            // when
            sagaOrchestrator.handleStockReserved(event);

            // then
            verify(outboxPublisher).saveEvent(
                    eq("ORDER"),
                    anyString(),
                    eq("STOCK_RESTORE_REQUESTED"),
                    eq("stock-restore-topic"),
                    any()
            );
        }

        @Test
        @DisplayName("중복 이벤트는 무시된다 (멱등성)")
        void handleStockReserved_DuplicateEvent_ShouldBeIgnored() {
            // given
            StockReservedEvent event = StockReservedEvent.builder()
                    .orderId("1")
                    .productId("1")
                    .quantity(2)
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(true);

            // when
            sagaOrchestrator.handleStockReserved(event);

            // then
            verify(orderRepository, never()).findById(anyLong());
            verify(outboxPublisher, never()).saveEvent(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("재고 예약 실패 처리")
    class HandleStockReservationFailedTest {

        @Test
        @DisplayName("재고 예약 실패 시 주문이 CANCELLED 상태가 된다")
        void handleStockReservationFailed_ShouldCancelOrder() {
            // given
            StockReservationFailedEvent event = StockReservationFailedEvent.builder()
                    .orderId("1")
                    .productId("1")
                    .quantity(2)
                    .reason("재고 부족")
                    .status("STOCK_RESERVATION_FAILED")
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(orderRepository.findById(1L)).willReturn(Optional.of(mockOrder));
            given(orderRepository.save(any(Orders.class))).willReturn(mockOrder);

            // when
            sagaOrchestrator.handleStockReservationFailed(event);

            // then
            assertThat(mockOrder.getOrderStatus()).isEqualTo("CANCELLED");
            assertThat(mockOrder.getFailedItemCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("재고 예약 실패 시 ORDER_CANCELLED 이벤트가 발행된다")
        void handleStockReservationFailed_ShouldPublishOrderCancelled() {
            // given
            StockReservationFailedEvent event = StockReservationFailedEvent.builder()
                    .orderId("1")
                    .productId("1")
                    .quantity(2)
                    .reason("재고 부족")
                    .status("STOCK_RESERVATION_FAILED")
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(orderRepository.findById(1L)).willReturn(Optional.of(mockOrder));
            given(orderRepository.save(any(Orders.class))).willReturn(mockOrder);

            // when
            sagaOrchestrator.handleStockReservationFailed(event);

            // then
            verify(outboxPublisher).saveEvent(
                    eq("ORDER"),
                    eq("1"),
                    eq("ORDER_CANCELLED"),
                    eq("order-cancelled-topic"),
                    any()
            );
        }

        @Test
        @DisplayName("이미 예약된 상품이 있으면 재고 복구 이벤트가 발행된다")
        void handleStockReservationFailed_HasReservedItems_ShouldRestoreStock() {
            // given
            mockOrder.setReservedItemCount(1);

            StockReservationFailedEvent event = StockReservationFailedEvent.builder()
                    .orderId("1")
                    .productId("2")
                    .quantity(1)
                    .reason("재고 부족")
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(orderRepository.findById(1L)).willReturn(Optional.of(mockOrder));
            given(orderRepository.save(any(Orders.class))).willReturn(mockOrder);

            // when
            sagaOrchestrator.handleStockReservationFailed(event);

            // then
            verify(outboxPublisher, atLeastOnce()).saveEvent(
                    eq("ORDER"),
                    anyString(),
                    eq("STOCK_RESTORE_REQUESTED"),
                    eq("stock-restore-topic"),
                    any()
            );
        }
    }

    @Nested
    @DisplayName("결제 완료 처리")
    class HandlePaymentCompletedTest {

        @Test
        @DisplayName("결제 성공 시 주문이 COMPLETED 상태가 된다")
        void handlePaymentCompleted_Success_ShouldCompleteOrder() {
            // given
            mockOrder.setOrderStatus("STOCK_RESERVED");

            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .orderId("1")
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .amount(35000)
                    .success(true)
                    .status("PAYMENT_COMPLETED")
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(orderRepository.findById(1L)).willReturn(Optional.of(mockOrder));
            given(orderRepository.save(any(Orders.class))).willReturn(mockOrder);

            // when
            sagaOrchestrator.handlePaymentCompleted(event);

            // then
            assertThat(mockOrder.getOrderStatus()).isEqualTo("COMPLETED");
            verify(outboxPublisher).saveEvent(
                    eq("ORDER"),
                    eq("1"),
                    eq("ORDER_COMPLETED"),
                    eq("order-completed-topic"),
                    any()
            );
        }

        @Test
        @DisplayName("결제 실패 시 재고 복구 이벤트가 발행된다 (보상 트랜잭션)")
        void handlePaymentCompleted_Failed_ShouldTriggerCompensation() {
            // given
            mockOrder.setOrderStatus("STOCK_RESERVED");

            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .orderId("1")
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .amount(35000)
                    .success(false)
                    .status("PAYMENT_COMPLETED")
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(orderRepository.findById(1L)).willReturn(Optional.of(mockOrder));
            given(orderRepository.save(any(Orders.class))).willReturn(mockOrder);

            // when
            sagaOrchestrator.handlePaymentCompleted(event);

            // then
            assertThat(mockOrder.getOrderStatus()).isEqualTo("PAYMENT_FAILED");
            verify(outboxPublisher).saveEvent(
                    eq("ORDER"),
                    anyString(),
                    eq("STOCK_RESTORE_REQUESTED"),
                    eq("stock-restore-topic"),
                    any()
            );
        }
    }

    @Nested
    @DisplayName("결제 실패 처리")
    class HandlePaymentFailedTest {

        @Test
        @DisplayName("결제 실패 이벤트 수신 시 보상 트랜잭션이 실행된다")
        void handlePaymentFailed_ShouldTriggerCompensation() {
            // given
            mockOrder.setOrderStatus("STOCK_RESERVED");

            PaymentFailedEvent event = PaymentFailedEvent.builder()
                    .orderId("1")
                    .userId("1")
                    .productId("1")
                    .quantity(2)
                    .reason("카드 한도 초과")
                    .status("PAYMENT_FAILED")
                    .build();

            given(processedEventRepository.existsById(anyString())).willReturn(false);
            given(orderRepository.findById(1L)).willReturn(Optional.of(mockOrder));
            given(orderRepository.save(any(Orders.class))).willReturn(mockOrder);

            // when
            sagaOrchestrator.handlePaymentFailed(event);

            // then
            assertThat(mockOrder.getOrderStatus()).isEqualTo("PAYMENT_FAILED");
            verify(outboxPublisher).saveEvent(
                    eq("ORDER"),
                    anyString(),
                    eq("STOCK_RESTORE_REQUESTED"),
                    eq("stock-restore-topic"),
                    any()
            );
        }
    }
}
