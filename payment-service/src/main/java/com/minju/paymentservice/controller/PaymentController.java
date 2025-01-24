package com.minju.paymentservice.controller;

import com.minju.paymentservice.dto.PaymentRequestDto;
import com.minju.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentService paymentService;


    // 결제 진입 API
    @PostMapping("/enter")
    public ResponseEntity<String> enterPayment(@RequestBody PaymentRequestDto paymentRequestDto) {
        boolean canProceed = paymentService.validatePaymentEntry(paymentRequestDto);
        if (canProceed) {
            return ResponseEntity.ok("Payment entry is valid.");
        } else {
            return ResponseEntity.badRequest().body("Payment entry is invalid.");
        }
    }

    // 결제 API
//    @PostMapping("/process")
//    public ResponseEntity<String> processPayment(@RequestBody PaymentRequestDto paymentRequestDto) {
//        boolean success = paymentService.processPayment(paymentRequestDto);
//        if (success) {
//            return ResponseEntity.ok("Payment succeeded.");
//        } else {
//            return ResponseEntity.ok("Payment failed.");
//        }
//    }
    
    // 결제 처리
    @PostMapping("/process")
    public ResponseEntity<String> processPayment(@RequestBody PaymentRequestDto requestDto) {
        boolean isSuccess = paymentService.processPayment(requestDto);
        return ResponseEntity.ok(isSuccess ? "결제 성공" : "결제 실패");
    }
}
