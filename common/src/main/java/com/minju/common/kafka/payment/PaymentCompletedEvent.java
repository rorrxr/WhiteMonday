package com.minju.common.kafka.payment;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent {
    private String orderId;
    private String userId;
    private String productId;
    private int quantity;
    private int amount;
    private String status;
    private boolean success;
}