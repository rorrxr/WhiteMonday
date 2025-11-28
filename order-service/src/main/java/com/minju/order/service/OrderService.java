package com.minju.order.service;

import com.minju.common.dto.CartResponseDto;
import com.minju.common.kafka.StockReservationRequestEvent;
import com.minju.common.kafka.stock.StockRestoreEvent;
import com.minju.order.client.CartServiceClient;
import com.minju.order.client.ProductServiceClient;
import com.minju.order.dto.*;
import com.minju.order.entity.OrderItem;
import com.minju.order.entity.Orders;
import com.minju.order.outbox.OutboxEventPublisher;
import com.minju.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final String PRODUCT_CB = "productService";
    private static final String CART_CB = "cartService";

    private final OrderRepository orderRepository;
    private final OutboxEventPublisher outboxPublisher;
    private final CartServiceClient cartServiceClient;
    private final ProductServiceClient productServiceClient;

    /**
     * 장바구니에서 주문 생성 - (Outbox 패턴 + Circuit Breaker)
     */
    @CircuitBreaker(name = CART_CB, fallbackMethod = "createOrderFromCartFallback")
    @Retry(name = CART_CB, fallbackMethod = "createOrderFromCartRetryFallback")
    @RateLimiter(name = CART_CB)
    @Transactional
    public OrderResponseDto createOrderFromCart(Long userId) {
        log.info("주문 생성 시작 - userId: {}", userId);

        // 1. Cart Service에서 장바구니 조회 (FeignClient - 동기 호출)
        CartResponseDto cart = getCartWithCircuitBreaker(userId);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("장바구니에 상품이 없습니다.");
        }

        // 2. 주문 생성 (PENDING 상태)
        Orders order = new Orders();
        order.setUserId(userId);
        order.setOrderStatus("PENDING");

        int totalAmount = 0;
        for (CartResponseDto.CartItemDto cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getSubtotal()); // 장바구니에서 이미 계산된 가격 사용
            totalAmount += orderItem.getPrice();

            order.getOrderItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        Orders savedOrder = orderRepository.save(order);

        // 같은 트랜잭션 내에서 Outbox 이벤트 저장
        for (OrderItem orderItem : savedOrder.getOrderItems()) {
            StockReservationRequestEvent event = StockReservationRequestEvent.builder()
                    .orderId(String.valueOf(savedOrder.getId()))
                    .productId(String.valueOf(orderItem.getProductId()))
                    .quantity(orderItem.getQuantity())
                    .status("STOCK_RESERVATION_REQUESTED")
                    .build();

            outboxPublisher.saveEvent(
                    "ORDER",
                    String.valueOf(savedOrder.getId()),
                    "STOCK_RESERVATION_REQUESTED",
                    "stock-reservation-requested-topic",
                    event
            );
        }

        // 장바구니 비우기 (비동기로 처리 - 실패해도 주문은 생성됨)
        try {
            clearCartAsync(userId);
        } catch (Exception e) {
            log.warn("장바구니 비우기 실패 (주문은 생성됨) - userId: {}, error: {}",
                    userId, e.getMessage());
            // 장바구니 비우기 실패는 주문 생성을 막지 않음
        }

        log.info("주문 생성 및 Outbox 이벤트 저장 완료 - orderId: {}", savedOrder.getId());
        return new OrderResponseDto(savedOrder);
    }

    /**
     * 장바구니 조회 with Circuit Breaker
     */
    @CircuitBreaker(name = CART_CB, fallbackMethod = "getCartFallback")
    @Retry(name = CART_CB)
    private CartResponseDto getCartWithCircuitBreaker(Long userId) {
        log.debug("장바구니 조회 시도 - userId: {}", userId);
        return cartServiceClient.getCart(userId);
    }

    /**
     * 장바구니 비우기 (비동기 - 실패해도 주문 생성에 영향 없음)
     */
    @CircuitBreaker(name = CART_CB, fallbackMethod = "clearCartFallback")
    private void clearCartAsync(Long userId) {
        cartServiceClient.clearCart(userId);
        log.info("장바구니 비우기 완료 - userId: {}", userId);
    }

    // ==================== Circuit Breaker Fallback Methods ====================

    /**
     * 주문 생성 Circuit Breaker Fallback
     */
    public OrderResponseDto createOrderFromCartFallback(Long userId, Exception ex) {
        log.error("주문 생성 Circuit Breaker 활성화 - userId: {}, error: {}",
                userId, ex.getMessage());
        return createFallbackOrderResponse("서비스 일시 중단 - 잠시 후 다시 시도해주세요");
    }

    /**
     * 주문 생성 Retry Fallback
     */
    public OrderResponseDto createOrderFromCartRetryFallback(Long userId, Exception ex) {
        log.error("주문 생성 재시도 실패 - userId: {}, error: {}", userId, ex.getMessage());
        return createFallbackOrderResponse("재시도 실패 - 잠시 후 다시 시도해주세요");
    }

    /**
     * 장바구니 조회 Fallback
     */
    public CartResponseDto getCartFallback(Long userId, Exception ex) {
        log.error("장바구니 조회 Fallback 활성화 - userId: {}, error: {}",
                userId, ex.getMessage());
        throw new RuntimeException("장바구니 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }

    /**
     * 장바구니 비우기 Fallback (실패해도 무시)
     */
    public void clearCartFallback(Long userId, Exception ex) {
        log.warn("장바구니 비우기 Fallback - userId: {}, error: {}",
                userId, ex.getMessage());
        // 장바구니 비우기 실패는 로그만 남기고 무시
    }

    private OrderResponseDto createFallbackOrderResponse(String reason) {
        Orders fallbackOrder = new Orders();
        fallbackOrder.setId(-1L);
        fallbackOrder.setOrderStatus("FAILED");
        fallbackOrder.setUserId(-1L);
        fallbackOrder.setTotalAmount(0);
        return new OrderResponseDto(fallbackOrder);
    }

    // ==================== 기존 메서드들 ====================

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrders(Long userId) {
        List<Orders> orders = orderRepository.findByUserId(userId);
        return orders.stream().map(OrderResponseDto::new).collect(Collectors.toList());
    }

    /**
     * 주문 취소 - Outbox를 통한 재고 복구
     */
    @Transactional
    public OrderResponseDto cancelOrder(Long orderId, Long userId) {
        Orders order = getOrder(orderId, userId);
        validateOrderStatus(order, List.of("PENDING", "PROCESSING", "STOCK_RESERVED"));

        order.setOrderStatus("CANCELLED");
        orderRepository.save(order);

        // Outbox를 통한 재고 복구 이벤트 발행
        publishStockRestoreEvent(order, "주문 취소");

        return new OrderResponseDto(order);
    }

    /**
     * 반품 - Outbox를 통한 재고 복구
     */
    @Transactional
    public OrderResponseDto returnOrder(Long orderId, Long userId) {
        Orders order = getOrder(orderId, userId);
        validateOrderStatus(order, List.of("DELIVERED"));

        order.setOrderStatus("RETURNED");
        orderRepository.save(order);

        // Outbox를 통한 재고 복구 이벤트 발행
        publishStockRestoreEvent(order, "반품");

        return new OrderResponseDto(order);
    }

    /**
     * 재고 복구 이벤트 발행 (Outbox 패턴)
     */
    private void publishStockRestoreEvent(Orders order, String reason) {
        for (OrderItem orderItem : order.getOrderItems()) {
            StockRestoreEvent event =
                    StockRestoreEvent.builder()
                            .orderId(String.valueOf(order.getId()))
                            .productId(String.valueOf(orderItem.getProductId()))
                            .quantity(orderItem.getQuantity())
                            .reason(reason)
                            .status("STOCK_RESTORE_REQUESTED")
                            .build();

            outboxPublisher.saveEvent(
                    "ORDER",
                    String.valueOf(order.getId()),
                    "STOCK_RESTORE_REQUESTED",
                    "stock-restore-topic",
                    event
            );
        }
        log.info("재고 복구 Outbox 이벤트 저장 완료 - orderId: {}, reason: {}",
                order.getId(), reason);
    }

    /**
     * 주문 상태 자동 업데이트 (스케줄러)
     */
    @Transactional
    public void updateOrderStatus() {
        List<Orders> orders = orderRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        orders.forEach(order -> {
            if ("PENDING".equals(order.getOrderStatus()) &&
                    order.getCreatedAt().plusDays(1).isBefore(now)) {
                order.setOrderStatus("SHIPPING");
            } else if ("SHIPPING".equals(order.getOrderStatus()) &&
                    order.getCreatedAt().plusDays(2).isBefore(now)) {
                order.setOrderStatus("DELIVERED");
            }
        });

        orderRepository.saveAll(orders);
    }

    private Orders getOrder(Long orderId, Long userId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문이 존재하지 않습니다."));
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        return order;
    }

    private void validateOrderStatus(Orders order, List<String> allowedStatuses) {
        if (!allowedStatuses.contains(order.getOrderStatus())) {
            throw new IllegalStateException("현재 상태에서는 작업을 수행할 수 없습니다: " + order.getOrderStatus());
        }
    }
}