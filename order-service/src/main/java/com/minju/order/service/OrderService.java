package com.minju.order.service;

import com.minju.common.dto.ProductDto;
import com.minju.order.client.ProductServiceClient;
import com.minju.order.dto.OrderRequestDto;
import com.minju.order.dto.OrderResponseDto;
import com.minju.order.entity.Orders;
import com.minju.order.entity.OrderItem;
import com.minju.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
    private final ProductServiceClient productServiceClient;
    private final OrderRepository orderRepository;

    @Transactional
    @CircuitBreaker(name = PRODUCT_CB, fallbackMethod = "createOrderFallback")
    public OrderResponseDto createOrder(Long userId, OrderRequestDto requestDto) {
        log.info("createOrder - userId: {}, requestDto: {}", userId, requestDto);

        // 주문 생성
        Orders order = new Orders();
        order.setUserId(userId);
        order.setOrderStatus("PENDING");
        orderRepository.save(order);

        int totalAmount = 0;

        // 각 상품에 대해 재고 확인 및 주문 항목 생성
        for (OrderRequestDto.Item item : requestDto.getItems()) {
            log.info("Processing item - productId: {}, quantity: {}", item.getProductId(), item.getQuantity());

            ProductDto product = productServiceClient.getProductById(item.getProductId());
            if (product.getStock() < item.getQuantity()) {
                throw new IllegalArgumentException("재고가 부족합니다: " + product.getTitle());
            }

            productServiceClient.decreaseStock(item.getProductId(), item.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(product.getPrice() * item.getQuantity());
            totalAmount += orderItem.getPrice();

            order.getOrderItems().add(orderItem);
        }

        // 총 금액 업데이트
        order.setTotalAmount(totalAmount);
        orderRepository.save(order);

        log.info("Order created successfully - orderId: {}", order.getId());
        return new OrderResponseDto(order);
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

    private ProductDto validateProductStock(Long productId, int quantity) {
        ProductDto product = productServiceClient.getProductById(productId);
        if (product.getStock() < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다: " + product.getTitle());
        }
        return product;
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

    private OrderItem createOrderItem(Orders order, ProductDto product, OrderRequestDto.Item item) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProductId(item.getProductId());
        orderItem.setQuantity(item.getQuantity());
        orderItem.setPrice(product.getPrice() * item.getQuantity());
        return orderItem;
    }

    public OrderResponseDto createOrderFallback(Long userId, OrderRequestDto requestDto, Throwable t) {
        log.error("ProductService unavailable. Fallback activated. Error: {}", t.toString());
        throw new IllegalStateException("상품 정보를 불러올 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
}
