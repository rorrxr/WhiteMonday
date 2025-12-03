package com.minju.paymentservice.controller;

import com.minju.common.dto.CommonResponse;
import com.minju.common.exception.ErrorCode;
import com.minju.common.kafka.payment.PaymentRequestedEvent;
import com.minju.paymentservice.dto.PaymentRequestDto;
import com.minju.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    // ================== 결제 진입 검증 API ==================
    @PostMapping("/enter")
    public ResponseEntity<CommonResponse<Void>> enterPayment(@RequestBody PaymentRequestDto paymentRequestDto) {
        boolean canProceed = paymentService.validatePaymentEntry(paymentRequestDto);

        if (canProceed) {
            return ResponseEntity.ok(
                    CommonResponse.success("결제 진입 검증에 성공했습니다.", null)
            );
        } else {
            ErrorCode errorCode = ErrorCode.INVALID_PAYMENT_STATUS;

            return ResponseEntity
                    .status(errorCode.getHttpStatus())
                    .body(CommonResponse.error(
                            errorCode.getHttpStatus().value(),
                            errorCode.getCode(),
                            errorCode.getMessage()
                    ));
        }
    }

    // ================== 동기 결제 처리 API (테스트/관리용) ==================
    @PostMapping("/process")
    public ResponseEntity<CommonResponse<Void>> processPayment(@RequestBody PaymentRequestDto requestDto) {

        // 1. DTO → SAGA 이벤트 변환 (Kafka 없이 직접 SAGA 로직 태우기)
        PaymentRequestedEvent event = PaymentRequestedEvent.builder()
                .orderId(requestDto.getOrderId().toString())
                .userId(requestDto.getUserId().toString())
                .productId(requestDto.getProductId() != null ? requestDto.getProductId().toString() : null)
                .quantity(requestDto.getQuantity())
                .amount(requestDto.getAmount())
                .status("PAYMENT_REQUESTED")
                .build();

        // 2. SAGA용 결제 처리 로직 재사용
        boolean isSuccess = paymentService.processPayment(event);

        // 3. 응답 변환
        if (isSuccess) {
            return ResponseEntity.ok(
                    CommonResponse.success("결제가 성공적으로 처리되었습니다.", null)
            );
        } else {
            ErrorCode errorCode = ErrorCode.FAILED_PAYMENT;

            return ResponseEntity
                    .status(errorCode.getHttpStatus())
                    .body(CommonResponse.error(
                            errorCode.getHttpStatus().value(),
                            errorCode.getCode(),
                            errorCode.getMessage()
                    ));
        }
    }
}
