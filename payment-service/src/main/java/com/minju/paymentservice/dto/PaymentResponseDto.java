package com.minju.paymentservice.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PaymentResponseDto {
    private Long Id;

    private String paymentStatus;
    private LocalDateTime paymentDate;
    private String paymentMethod;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
