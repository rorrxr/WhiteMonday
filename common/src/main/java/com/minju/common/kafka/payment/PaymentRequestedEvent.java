package com.minju.common.kafka.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestedEvent {
    private String orderId;
    private String userId;
    private String productId;
    private int quantity;
    private int amount;
    private String status; // PAYMENT_REQUESTED
}