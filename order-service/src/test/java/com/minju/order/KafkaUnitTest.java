package com.minju.order;

import com.minju.common.kafka.*;
import com.minju.order.entity.Orders;
import com.minju.order.repository.OrderRepository;
import com.minju.order.saga.SagaOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaUnitTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private SagaOrchestrator sagaOrchestrator;

    @Test
    void testHandleStockReserved_Success() {
        // Given
        StockReservedEvent event = StockReservedEvent.builder()
                .orderId("123")
                .productId("product-456")
                .quantity(5)
                .status("STOCK_RESERVED")
                .build();

        Orders order = Orders.builder()
                .id(123L)
                .userId(1L)
                .totalAmount(50000) // Integer로 변경
                .orderStatus("PENDING")
                .build();

        when(orderRepository.findById(123L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Orders.class))).thenReturn(order);

        // When
        sagaOrchestrator.handleStockReserved(event);

        // Then
        // 1. 주문 상태 업데이트 검증
        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderStatus()).isEqualTo("STOCK_RESERVED");

        // 2. PaymentRequestedEvent 발행 검증
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("payment-requested-topic");
        assertThat(eventCaptor.getValue()).isInstanceOf(PaymentRequestedEvent.class);

        PaymentRequestedEvent paymentEvent = (PaymentRequestedEvent) eventCaptor.getValue();
        assertThat(paymentEvent.getOrderId()).isEqualTo("123");
        assertThat(paymentEvent.getUserId()).isEqualTo("1");
        assertThat(paymentEvent.getProductId()).isEqualTo("product-456");
        assertThat(paymentEvent.getQuantity()).isEqualTo(5);
        assertThat(paymentEvent.getAmount()).isEqualTo(50000); // Integer로 변경
        assertThat(paymentEvent.getStatus()).isEqualTo("PAYMENT_REQUESTED");
    }

    @Test
    void testHandleStockReserved_OrderNotFound() {
        // Given
        StockReservedEvent event = StockReservedEvent.builder()
                .orderId("999")
                .productId("product-999")
                .quantity(1)
                .status("STOCK_RESERVED")
                .build();

        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        sagaOrchestrator.handleStockReserved(event);

        // Then
        // 주문을 찾을 수 없어서 StockRestoreEvent가 발행되어야 함
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("stock-restore-topic");
        assertThat(eventCaptor.getValue()).isInstanceOf(StockRestoreEvent.class);

        StockRestoreEvent restoreEvent = (StockRestoreEvent) eventCaptor.getValue();
        assertThat(restoreEvent.getOrderId()).isEqualTo("999");
        assertThat(restoreEvent.getProductId()).isEqualTo("product-999");
        assertThat(restoreEvent.getQuantity()).isEqualTo(1);
        assertThat(restoreEvent.getReason()).isEqualTo("SAGA 처리 오류");
    }

    @Test
    void testHandleStockReservationFailed() {
        // Given
        StockReservationFailedEvent event = StockReservationFailedEvent.builder()
                .orderId("123")
                .productId("product-456")
                .quantity(5)
                .reason("재고 부족")
                .build();

        Orders order = Orders.builder()
                .id(123L)
                .orderStatus("PENDING")
                .build();

        when(orderRepository.findById(123L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Orders.class))).thenReturn(order);

        // When
        sagaOrchestrator.handleStockReservationFailed(event);

        // Then
        // 1. 주문 상태를 CANCELLED로 업데이트
        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderStatus()).isEqualTo("CANCELLED");

        // 2. OrderCancelledEvent 발행
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("order-cancelled-topic");
        assertThat(eventCaptor.getValue()).isInstanceOf(OrderCancelledEvent.class);

        OrderCancelledEvent cancelEvent = (OrderCancelledEvent) eventCaptor.getValue();
        assertThat(cancelEvent.getOrderId()).isEqualTo("123");
        assertThat(cancelEvent.getReason()).isEqualTo("재고 부족: 재고 부족");
        assertThat(cancelEvent.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void testHandlePaymentCompleted_Success() {
        // Given
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId("123")
                .productId("product-456")
                .quantity(5)
                .amount(50000) // Integer로 변경
                .success(true)
                .build();

        Orders order = Orders.builder()
                .id(123L)
                .orderStatus("STOCK_RESERVED")
                .build();

        when(orderRepository.findById(123L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Orders.class))).thenReturn(order);

        // When
        sagaOrchestrator.handlePaymentCompleted(event);

        // Then
        // 1. 주문 상태를 COMPLETED로 업데이트
        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderStatus()).isEqualTo("COMPLETED");

        // 2. OrderCompletedEvent 발행
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("order-completed-topic");
        assertThat(eventCaptor.getValue()).isInstanceOf(OrderCompletedEvent.class);

        OrderCompletedEvent completedEvent = (OrderCompletedEvent) eventCaptor.getValue();
        assertThat(completedEvent.getOrderId()).isEqualTo("123");
        assertThat(completedEvent.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void testHandlePaymentCompleted_Failed() {
        // Given
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId("123")
                .productId("product-456")
                .quantity(5)
                .amount(50000) // Integer로 변경
                .success(false) // 결제 실패
                .build();

        Orders order = Orders.builder()
                .id(123L)
                .orderStatus("STOCK_RESERVED")
                .build();

        when(orderRepository.findById(123L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Orders.class))).thenReturn(order);

        // When
        sagaOrchestrator.handlePaymentCompleted(event);

        // Then
        // 1. 주문 상태를 PAYMENT_FAILED로 업데이트
        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderStatus()).isEqualTo("PAYMENT_FAILED");

        // 2. StockRestoreEvent 발행
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("stock-restore-topic");
        assertThat(eventCaptor.getValue()).isInstanceOf(StockRestoreEvent.class);

        StockRestoreEvent restoreEvent = (StockRestoreEvent) eventCaptor.getValue();
        assertThat(restoreEvent.getOrderId()).isEqualTo("123");
        assertThat(restoreEvent.getProductId()).isEqualTo("product-456");
        assertThat(restoreEvent.getQuantity()).isEqualTo(5);
        assertThat(restoreEvent.getReason()).isEqualTo("결제 실패");
    }

    @Test
    void testHandlePaymentFailed() {
        // Given
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .orderId("123")
                .productId("product-456")
                .quantity(5)
                .reason("카드 한도 초과")
                .build();

        Orders order = Orders.builder()
                .id(123L)
                .orderStatus("STOCK_RESERVED")
                .build();

        when(orderRepository.findById(123L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Orders.class))).thenReturn(order);

        // When
        sagaOrchestrator.handlePaymentFailed(event);

        // Then
        // 1. 주문 상태를 PAYMENT_FAILED로 업데이트
        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderStatus()).isEqualTo("PAYMENT_FAILED");

        // 2. StockRestoreEvent 발행
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("stock-restore-topic");
        assertThat(eventCaptor.getValue()).isInstanceOf(StockRestoreEvent.class);

        StockRestoreEvent restoreEvent = (StockRestoreEvent) eventCaptor.getValue();
        assertThat(restoreEvent.getOrderId()).isEqualTo("123");
        assertThat(restoreEvent.getProductId()).isEqualTo("product-456");
        assertThat(restoreEvent.getQuantity()).isEqualTo(5);
        assertThat(restoreEvent.getReason()).isEqualTo("카드 한도 초과");
    }

    @Test
    void testTransactionRollback() {
        // Given
        StockReservedEvent event = StockReservedEvent.builder()
                .orderId("123")
                .productId("product-456")
                .quantity(5)
                .status("STOCK_RESERVED")
                .build();

        Orders order = Orders.builder()
                .id(123L)
                .userId(1L)
                .totalAmount(50000)
                .orderStatus("PENDING")
                .build();

        when(orderRepository.findById(123L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Orders.class))).thenReturn(order);
        // KafkaTemplate.send()에서 예외 발생
        when(kafkaTemplate.send(eq("payment-requested-topic"), any()))
                .thenThrow(new RuntimeException("Kafka connection failed"));

        // When
        sagaOrchestrator.handleStockReserved(event);

        // Then
        // 예외가 발생했으므로 StockRestoreEvent가 발행되어야 함
        verify(kafkaTemplate, times(2)).send(any(String.class), any(Object.class));

        // 첫 번째 호출은 payment-requested-topic (실패)
        // 두 번째 호출은 stock-restore-topic (보상 처리)
    }
}