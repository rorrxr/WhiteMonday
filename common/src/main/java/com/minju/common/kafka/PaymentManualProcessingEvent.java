package com.minju.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentManualProcessingEvent {
    private String orderId;
    private int amount;
    private String reason;
    private String status; // MANUAL_PROCESSING_REQUIRED
}