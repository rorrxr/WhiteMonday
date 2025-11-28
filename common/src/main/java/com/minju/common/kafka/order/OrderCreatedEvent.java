package com.minju.common.kafka.order;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {
    private Long orderId;
    private Long userId;
    private Long productId;
    private int quantity;
    private int amount;
    private String status;
}