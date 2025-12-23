package com.minju.order.service;

import com.minju.common.dto.CartResponseDto;
import com.minju.common.kafka.stock.StockReservationRequestEvent;
import com.minju.order.client.CartServiceClient;
import com.minju.order.client.ProductServiceClient;
import com.minju.order.dto.OrderResponseDto;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventPublisher outboxPublisher;

    @Mock
    private CartServiceClient cartServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private OrderService orderService;

    private CartResponseDto mockCart;
    private Orders mockOrder;

    @BeforeEach
    void setUp() {
        // 장바구니 Mock 데이터
        CartResponseDto.CartItemDto item1 = new CartResponseDto.CartItemDto();
        item1.setProductId(1L);
        item1.setQuantity(2);
        item1.setSubtotal(20000);

        CartResponseDto.CartItemDto item2 = new CartResponseDto.CartItemDto();
        item2.setProductId(2L);
        item2.setQuantity(1);
        item2.setSubtotal(15000);

        mockCart = new CartResponseDto();
        mockCart.setUserId(1L);
        mockCart.setItems(List.of(item1, item2));

        // 주문 Mock 데이터
        mockOrder = new Orders();
        mockOrder.setId(1L);
        mockOrder.setUserId(1L);
        mockOrder.setOrderStatus("PENDING");
        mockOrder.setTotalAmount(35000);
        mockOrder.setTotalItemCount(2);
        mockOrder.setReservedItemCount(0);
        mockOrder.setFailedItemCount(0);
    }

    @Nested
    @DisplayName("주문 생성 테스트")
    class CreateOrderTest {

        @Test
        @DisplayName("장바구니에서 주문 생성 시 Outbox 이벤트가 저장된다")
        void createOrderFromCart_ShouldSaveOutboxEvents() {
            // given
            given(cartServiceClient.getCart(1L)).willReturn(mockCart);
            given(orderRepository.save(any(Orders.class))).willAnswer(invocation -> {
                Orders order = invocation.getArgument(0);
                order.setId(1L);
                return order;
            });
            willDoNothing().given(cartServiceClient).clearCart(1L);

            // when
            OrderResponseDto result = orderService.createOrderFromCart(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getOrderStatus()).isEqualTo("PENDING");

            // Outbox 이벤트 저장 검증 (장바구니 상품 수만큼 호출)
            verify(outboxPublisher, times(2)).saveEvent(
                    eq("ORDER"),
                    anyString(),
                    eq("STOCK_RESERVATION_REQUESTED"),
                    eq("stock-reservation-requested-topic"),
                    any(StockReservationRequestEvent.class)
            );
        }

        @Test
        @DisplayName("빈 장바구니로 주문 생성 시 예외가 발생한다")
        void createOrderFromCart_EmptyCart_ShouldThrowException() {
            // given
            CartResponseDto emptyCart = new CartResponseDto();
            emptyCart.setItems(new ArrayList<>());
            given(cartServiceClient.getCart(1L)).willReturn(emptyCart);

            // when & then
            assertThatThrownBy(() -> orderService.createOrderFromCart(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("장바구니에 상품이 없습니다");
        }

        @Test
        @DisplayName("주문 생성 시 totalItemCount가 올바르게 설정된다")
        void createOrderFromCart_ShouldSetCorrectTotalItemCount() {
            // given
            given(cartServiceClient.getCart(1L)).willReturn(mockCart);

            ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
            given(orderRepository.save(orderCaptor.capture())).willAnswer(invocation -> {
                Orders order = invocation.getArgument(0);
                order.setId(1L);
                return order;
            });

            // when
            orderService.createOrderFromCart(1L);

            // then
            Orders savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getTotalItemCount()).isEqualTo(2);
            assertThat(savedOrder.getReservedItemCount()).isEqualTo(0);
            assertThat(savedOrder.getFailedItemCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("주문 취소 테스트")
    class CancelOrderTest {

        @Test
        @DisplayName("PENDING 상태의 주문을 취소하면 재고 복구 이벤트가 발행된다")
        void cancelOrder_PendingOrder_ShouldPublishStockRestoreEvent() {
            // given
            mockOrder.setOrderStatus("PENDING");
            OrderItem item = new OrderItem();
            item.setProductId(1L);
            item.setQuantity(2);
            item.setOrder(mockOrder);
            mockOrder.getOrderItems().add(item);

            given(orderRepository.findById(1L)).willReturn(Optional.of(mockOrder));
            given(orderRepository.save(any(Orders.class))).willReturn(mockOrder);

            // when
            OrderResponseDto result = orderService.cancelOrder(1L, 1L);

            // then
            assertThat(result.getOrderStatus()).isEqualTo("CANCELLED");
            verify(outboxPublisher).saveEvent(
                    eq("ORDER"),
                    anyString(),
                    eq("STOCK_RESTORE_REQUESTED"),
                    eq("stock-restore-topic"),
                    any()
            );
        }

        @Test
        @DisplayName("COMPLETED 상태의 주문은 취소할 수 없다")
        void cancelOrder_CompletedOrder_ShouldThrowException() {
            // given
            mockOrder.setOrderStatus("COMPLETED");
            given(orderRepository.findById(1L)).willReturn(Optional.of(mockOrder));

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("현재 상태에서는 작업을 수행할 수 없습니다");
        }

        @Test
        @DisplayName("다른 사용자의 주문은 취소할 수 없다")
        void cancelOrder_DifferentUser_ShouldThrowException() {
            // given
            mockOrder.setUserId(1L);
            given(orderRepository.findById(1L)).willReturn(Optional.of(mockOrder));

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(1L, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("권한이 없습니다");
        }
    }

    @Nested
    @DisplayName("주문 상태 전이 테스트")
    class OrderStatusTransitionTest {

        @Test
        @DisplayName("모든 상품 재고 예약 완료 시 isAllItemsReserved가 true를 반환한다")
        void isAllItemsReserved_AllReserved_ShouldReturnTrue() {
            // given
            Orders order = new Orders();
            order.setTotalItemCount(3);
            order.setReservedItemCount(3);
            order.setFailedItemCount(0);

            // when & then
            assertThat(order.isAllItemsReserved()).isTrue();
        }

        @Test
        @DisplayName("일부 상품만 예약된 경우 isAllItemsReserved가 false를 반환한다")
        void isAllItemsReserved_PartialReserved_ShouldReturnFalse() {
            // given
            Orders order = new Orders();
            order.setTotalItemCount(3);
            order.setReservedItemCount(2);
            order.setFailedItemCount(0);

            // when & then
            assertThat(order.isAllItemsReserved()).isFalse();
        }

        @Test
        @DisplayName("재고 예약 실패 시 hasAnyFailedReservation이 true를 반환한다")
        void hasAnyFailedReservation_HasFailed_ShouldReturnTrue() {
            // given
            Orders order = new Orders();
            order.setTotalItemCount(3);
            order.setReservedItemCount(1);
            order.setFailedItemCount(1);

            // when & then
            assertThat(order.hasAnyFailedReservation()).isTrue();
        }

        @Test
        @DisplayName("모든 예약 처리 완료 시 isAllReservationsProcessed가 true를 반환한다")
        void isAllReservationsProcessed_AllProcessed_ShouldReturnTrue() {
            // given
            Orders order = new Orders();
            order.setTotalItemCount(3);
            order.setReservedItemCount(2);
            order.setFailedItemCount(1);

            // when & then
            assertThat(order.isAllReservationsProcessed()).isTrue();
        }
    }
}
