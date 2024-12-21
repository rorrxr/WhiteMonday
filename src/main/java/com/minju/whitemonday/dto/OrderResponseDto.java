package com.minju.whitemonday.dto;

import com.minju.whitemonday.entity.Order;
import com.minju.whitemonday.entity.OrderItem;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class OrderResponseDto {
    private Long orderId;
    private String orderStatus;
    private int totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDto> items;

    public OrderResponseDto(Order order) {
        this.orderId = order.getOrderId();
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
        private Long productId;
        private String productName;
        private int quantity;
        private int price;

        public OrderItemDto(OrderItem orderItem) {
            this.productId = orderItem.getProduct().getId();
            this.productName = orderItem.getProduct().getTitle();
            this.quantity = orderItem.getQuantity();
            this.price = orderItem.getPrice();
        }
    }
}

