package com.minju.paymentservice.dto;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentRequestDto {
    private Long productId; // 상품 ID (상품을 식별하기 위한 필드)
    private int quantity;   // 구매 수량 (재고 복구 시 필요한 필드)

    private String paymentStatus; // 결제 상태 (예: "결제 진행 중", "결제 완료")
    private LocalDateTime paymentDate; // 결제 날짜
    private String paymentMethod; // 결제 수단

    private LocalDateTime createdAt; // 생성 시간
    private LocalDateTime updatedAt; // 수정 시간
}
