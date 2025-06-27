package com.minju.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationRequestEvent {
    private String orderId;
    private String productId;
    private int quantity;
    private String status; // STOCK_RESERVATION_REQUESTED
}