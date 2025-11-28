package com.minju.common.kafka.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockRestoreEvent {
    private String orderId;
    private String productId;
    private int quantity;
    private String reason;
    private String status; // STOCK_RESTORE_REQUESTED
}