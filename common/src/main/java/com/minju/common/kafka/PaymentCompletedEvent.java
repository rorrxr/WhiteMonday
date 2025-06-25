package com.minju.common.kafka;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent {
    private Long orderId;
    private Long productId;
    private int quantity;
}