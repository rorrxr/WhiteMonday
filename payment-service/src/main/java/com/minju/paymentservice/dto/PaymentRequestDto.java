package com.minju.paymentservice.dto;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentRequestDto {
    private Long Id;

    private String paymentStatus;
    private LocalDateTime paymentDate;
    private String paymentMethod;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
