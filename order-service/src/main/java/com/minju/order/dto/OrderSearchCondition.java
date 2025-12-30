package com.minju.order.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderSearchCondition {
    private Long userId;
    private String orderStatus;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer minAmount;
    private Integer maxAmount;
}
