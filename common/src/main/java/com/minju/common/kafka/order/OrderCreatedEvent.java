package com.minju.common.kafka.order;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {
    private String orderId;
    private String userId;
    private String productId;
    private int quantity;
    private int amount;
    private String status;
}