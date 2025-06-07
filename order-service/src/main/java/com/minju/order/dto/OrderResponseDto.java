package com.minju.order.dto;

import com.minju.order.entity.Orders;
import com.minju.order.entity.OrderItem;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class OrderResponseDto {
    private final Long orderId;
    private final String orderStatus;
    private final int totalAmount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<OrderItemDto> items;

    public OrderResponseDto(Orders order) {
        this.orderId = order.getId(); // 엔티티에서 getId() 메서드를 호출
        this.orderStatus = order.getOrderStatus();
        this.totalAmount = order.getTotalAmount();
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
        this.items = order.getOrderItems() == null ? new ArrayList<>() :
                order.getOrderItems().stream()
                        .map(OrderItemDto::new)
                        .collect(Collectors.toList());
    }

    @Getter
    public static class OrderItemDto {
        private final Long productId;
        private final int quantity;
        private final int price;

        public OrderItemDto(OrderItem orderItem) {
            this.productId = orderItem.getProductId(); // OrderItem에서 productId 직접 참조
            this.quantity = orderItem.getQuantity();
            this.price = orderItem.getPrice();
        }
    }
}