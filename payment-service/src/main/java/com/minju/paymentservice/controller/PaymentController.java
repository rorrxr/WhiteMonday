package com.minju.paymentservice.controller;

import com.minju.common.dto.CommonResponse;
import com.minju.paymentservice.dto.PaymentRequestDto;
import com.minju.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentService paymentService;

    // 결제 진입 API
    @PostMapping("/enter")
    public ResponseEntity<CommonResponse<Void>> enterPayment(@RequestBody PaymentRequestDto paymentRequestDto) {
        boolean canProceed = paymentService.validatePaymentEntry(paymentRequestDto);

        if (canProceed) {
            return ResponseEntity.ok(
                    CommonResponse.success("결제 진입 검증에 성공했습니다.", null)
            );
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(
                            HttpStatus.BAD_REQUEST.value(),
                            "결제 진입 조건을 만족하지 못했습니다."
                    ));
        }
    }

    // 결제 처리
    @PostMapping("/process")
    public ResponseEntity<CommonResponse<Void>> processPayment(@RequestBody PaymentRequestDto requestDto) {
        boolean isSuccess = paymentService.processPayment(requestDto);

        if (isSuccess) {
            return ResponseEntity.ok(
                    CommonResponse.success("결제가 성공적으로 처리되었습니다.", null)
            );
        } else {
            // 400 혹은 402 Payment Required 등으로도 확장 가능
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(
                            HttpStatus.BAD_REQUEST.value(),
                            "결제 처리에 실패했습니다."
                    ));
        }
    }
}
