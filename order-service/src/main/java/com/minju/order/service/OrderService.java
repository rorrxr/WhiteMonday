package com.minju.order.service;

import com.minju.order.dto.*;
import com.minju.common.dto.ProductDto;
import com.minju.order.client.ProductServiceClient;
import com.minju.order.entity.Orders;
import com.minju.order.entity.OrderItem;
import com.minju.common.kafka.StockReservationRequestEvent;
import com.minju.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final String PRODUCT_CB = "productService";
    private static final String PAYMENT_CB = "paymentService";

    private final ProductServiceClient productServiceClient;
    private final OrderRepository orderRepository;
    // Object 타입으로 변경하여 다양한 이벤트 타입 지원
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @CircuitBreaker(name = PRODUCT_CB, fallbackMethod = "createOrderFallback")
    @Retry(name = PRODUCT_CB, fallbackMethod = "createOrderRetryFallback")
    @RateLimiter(name = PRODUCT_CB)
    @TimeLimiter(name = PRODUCT_CB)
    @Transactional
    public CompletableFuture<OrderResponseDto> createOrder(Long userId, OrderRequestDto requestDto) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("주문 생성 시작 - userId: {}", userId);

            // 1. 주문 생성 (PENDING 상태)
            Orders order = new Orders();
            order.setUserId(userId);
            order.setOrderStatus("PENDING");

            int totalAmount = 0;
            for (OrderRequestDto.Item item : requestDto.getItems()) {
                // 상품 정보 조회 (Circuit Breaker 적용)
                ProductDto product = getProductWithCircuitBreaker(item.getProductId());

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProductId(item.getProductId());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setPrice(product.getPrice() * item.getQuantity());
                totalAmount += orderItem.getPrice();

                order.getOrderItems().add(orderItem);
            }

            order.setTotalAmount(totalAmount);
            Orders savedOrder = orderRepository.save(order);

            // 2. SAGA 패턴으로 재고 예약 이벤트 발행
            publishStockReservationEvent(savedOrder, requestDto.getItems().get(0));

            return new OrderResponseDto(savedOrder);
        });
    }
    public void createOrderEvent(Long userId, OrderRequestDto requestDto) {
        // 1. DB에 PENDING 상태로 저장
        Orders order = new Orders();
        order.setUserId(userId);
        order.setOrderStatus("PENDING");

        int totalAmount = 0;
        for (OrderRequestDto.Item item : requestDto.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(item.getQuantity());
            // 가격은 나중에 동기화되도록 임시 0원으로 설정 가능
            orderItem.setPrice(0);
            order.getOrderItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        Orders savedOrder = orderRepository.save(order);

        // 2. Kafka 이벤트 발행 (재고 예약 요청)
        StockReservationRequestEvent event = StockReservationRequestEvent.builder()
                .orderId(String.valueOf(savedOrder.getId()))
                .productId(String.valueOf(requestDto.getItems().get(0).getProductId()))
                .quantity(requestDto.getItems().get(0).getQuantity())
                .status("STOCK_RESERVATION_REQUESTED")
                .build();

        kafkaTemplate.send("stock-reservation-requested-topic", event);
        log.info("주문 생성 및 재고 예약 이벤트 발행 완료: {}", event);
    }
    @CircuitBreaker(name = PRODUCT_CB, fallbackMethod = "getProductFallback")
    @Retry(name = PRODUCT_CB)
    private ProductDto getProductWithCircuitBreaker(Long productId) {
        log.debug("상품 조회 시도 - productId: {}", productId);
        return productServiceClient.getProductById(productId);
    }

    // Circuit Breaker Fallback Methods
    public CompletableFuture<OrderResponseDto> createOrderFallback(Long userId, OrderRequestDto requestDto, Exception ex) {
        log.error("주문 생성 Circuit Breaker 활성화 - userId: {}, error: {}", userId, ex.getMessage());
        return CompletableFuture.completedFuture(createFallbackOrderResponse("서비스 일시 중단"));
    }

    public CompletableFuture<OrderResponseDto> createOrderRetryFallback(Long userId, OrderRequestDto requestDto, Exception ex) {
        log.error("주문 생성 재시도 실패 - userId: {}, error: {}", userId, ex.getMessage());
        return CompletableFuture.completedFuture(createFallbackOrderResponse("재시도 실패"));
    }

    public ProductDto getProductFallback(Long productId, Exception ex) {
        log.error("상품 조회 Fallback 활성화 - productId: {}, error: {}", productId, ex.getMessage());
        return createFallbackProduct(productId);
    }

    private OrderResponseDto createFallbackOrderResponse(String reason) {
        Orders fallbackOrder = new Orders();
        fallbackOrder.setId(-1L);
        fallbackOrder.setOrderStatus("FAILED");
        return new OrderResponseDto(fallbackOrder);
    }

    private ProductDto createFallbackProduct(Long productId) {
        ProductDto fallbackProduct = new ProductDto();
        fallbackProduct.setProductId(productId);
        fallbackProduct.setTitle("상품 정보를 불러올 수 없습니다");
        fallbackProduct.setPrice(0);
        fallbackProduct.setStock(0);
        return fallbackProduct;
    }

    private void publishStockReservationEvent(Orders order, OrderRequestDto.Item item) {
        try {
            StockReservationRequestEvent event = StockReservationRequestEvent.builder()
                    .orderId(String.valueOf(order.getId()))
                    .productId(String.valueOf(item.getProductId()))
                    .quantity(item.getQuantity())
                    .status("STOCK_RESERVATION_REQUESTED")
                    .build();

            kafkaTemplate.send("stock-reservation-requested-topic", event);
            log.info("재고 예약 요청 이벤트 발행: {}", event);
        } catch (Exception e) {
            log.error("이벤트 발행 실패: ", e);
            // 이벤트 발행 실패시 주문 상태를 FAILED로 변경
            order.setOrderStatus("EVENT_PUBLISH_FAILED");
            orderRepository.save(order);
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrders(Long userId) {
        List<Orders> orders = orderRepository.findByUserId(userId);
        return orders.stream().map(OrderResponseDto::new).collect(Collectors.toList());
    }

    @Transactional
    public OrderResponseDto cancelOrder(Long orderId, Long userId) {
        Orders order = getOrder(orderId, userId);
        validateOrderStatus(order, List.of("PENDING", "PROCESSING"));

        order.setOrderStatus("CANCELLED");
        order.getOrderItems().forEach(orderItem ->
                productServiceClient.increaseStock(orderItem.getProductId(), orderItem.getQuantity()));

        orderRepository.save(order);
        return new OrderResponseDto(order);
    }

    @Transactional
    public OrderResponseDto returnOrder(Long orderId, Long userId) {
        Orders order = getOrder(orderId, userId);
        validateOrderStatus(order, List.of("DELIVERED"));

        order.setOrderStatus("RETURNED");
        order.getOrderItems().forEach(orderItem ->
                productServiceClient.increaseStock(orderItem.getProductId(), orderItem.getQuantity()));

        orderRepository.save(order);
        return new OrderResponseDto(order);
    }

    @Transactional
    public void updateOrderStatus() {
        List<Orders> orders = orderRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        orders.forEach(order -> {
            if ("PENDING".equals(order.getOrderStatus()) && order.getCreatedAt().plusDays(1).isBefore(now)) {
                order.setOrderStatus("SHIPPING");
            } else if ("SHIPPING".equals(order.getOrderStatus()) && order.getCreatedAt().plusDays(2).isBefore(now)) {
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
