package com.minju.whitemonday.order.service;

import com.minju.whitemonday.order.dto.OrderRequestDto;
import com.minju.whitemonday.order.dto.OrderResponseDto;
import com.minju.whitemonday.order.entity.Order;
import com.minju.whitemonday.order.entity.OrderItem;
import com.minju.whitemonday.order.repository.OrderItemRepository;
import com.minju.whitemonday.order.repository.OrderRepository;
import com.minju.product.entity.Product;
import com.minju.product.repository.ProductRepository;
import com.minju.whitemonday.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto requestDto, User user) {
        // Order 생성
        Order order = new Order();
        order.setUser(user);
        order.setOrderStatus("PENDING");
        order.setTotalAmount(0);
        order = orderRepository.save(order);

        int totalAmount = 0;

        // OrderItem 생성
        for (OrderRequestDto.Item item : requestDto.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order); // Order와 연결
            orderItem.setProduct(product);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(product.getPrice() * item.getQuantity());
            totalAmount += orderItem.getPrice();

            orderItemRepository.save(orderItem);

            // Order의 orderItems에 추가
            order.getOrderItems().add(orderItem);
        }

        // 총 금액 업데이트
        order.setTotalAmount(totalAmount);
        orderRepository.save(order);

        return new OrderResponseDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrders(User user) {
        List<Order> orders = orderRepository.findAllByUser(user);
        return orders.stream().map(OrderResponseDto::new).collect(Collectors.toList());
    }

    @Transactional
    public void cancelOrder(Long orderId, User user) {
        Order order = orderRepository.findByOrderIdAndUser(orderId, user)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문을 찾을 수 없습니다."));

        if ("SHIPPING".equals(order.getOrderStatus())) {
            throw new IllegalStateException("배송 중인 주문은 취소할 수 없습니다.");
        }

        if ("DELIVERED".equals(order.getOrderStatus())) {
            throw new IllegalStateException("배송 완료된 주문은 취소할 수 없습니다.");
        }

        order.setOrderStatus("CANCELLED");

        // 재고 복구
        order.getOrderItems().forEach(orderItem -> {
            Product product = orderItem.getProduct();
            product.setStock(product.getStock() + orderItem.getQuantity());
            productRepository.save(product);
        });

        orderRepository.save(order);
    }

    @Transactional
    public void returnOrder(Long orderId, User user) {
        Order order = orderRepository.findByOrderIdAndUser(orderId, user)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문을 찾을 수 없습니다."));

        if (!"DELIVERED".equals(order.getOrderStatus())) {
            throw new IllegalStateException("배송 완료된 주문만 반품할 수 있습니다.");
        }

        if (order.getUpdatedAt().plusDays(1).isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("반품 가능 기간이 지났습니다.");
        }

        order.setOrderStatus("RETURNED");

        // 재고 복구는 D+1에 진행
        order.getOrderItems().forEach(orderItem -> {
            Product product = orderItem.getProduct();
            product.setStock(product.getStock() + orderItem.getQuantity());
            productRepository.save(product);
        });

        orderRepository.save(order);
    }

    @Transactional
    public void updateOrderStatus() {
        List<Order> orders = orderRepository.findAll();

        LocalDateTime now = LocalDateTime.now();

        for (Order order : orders) {
            if ("PENDING".equals(order.getOrderStatus()) && order.getCreatedAt().plusDays(1).isBefore(now)) {
                order.setOrderStatus("SHIPPING");
            } else if ("SHIPPING".equals(order.getOrderStatus()) && order.getCreatedAt().plusDays(2).isBefore(now)) {
                order.setOrderStatus("DELIVERED");
            }
        }

        // 변경된 주문 상태를 저장
        orderRepository.saveAll(orders);
    }
}
