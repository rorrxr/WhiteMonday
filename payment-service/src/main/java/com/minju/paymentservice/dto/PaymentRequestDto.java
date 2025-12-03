package com.minju.paymentservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentRequestDto {

    // SAGA 이벤트와 공통으로 사용할 필드
    private Long orderId;     // 주문 ID
    private Long userId;      // 사용자 ID
    private Long productId;   // 상품 ID
    private int quantity;     // 구매 수량
    private int amount;       // 결제 금액

    // 동기/프론트에서만 사용하는 보조 정보
    private String paymentStatus;   // 예: "결제 진행 중"
    private LocalDateTime paymentDate;
    private String paymentMethod;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
