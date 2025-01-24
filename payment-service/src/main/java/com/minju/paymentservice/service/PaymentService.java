package com.minju.paymentservice.service;

import com.minju.paymentservice.client.ProductServiceFeignClient;
import com.minju.paymentservice.dto.PaymentRequestDto;
import com.minju.paymentservice.entity.Payment;
import com.minju.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {


    private final PaymentRepository paymentRepository;
    private final ProductServiceFeignClient productServiceFeignClient;

    // 결제 처리 로직
//    @Transactional
//    public boolean processPayment(PaymentRequestDto paymentRequestDto) {
//        // 20% 확률로 결제 실패 시뮬레이션
//        boolean isSuccess = new Random().nextInt(100) >= 20;
//
//        // Payment 객체 생성 및 저장
//        Payment payment = new Payment();
//        payment.setPaymentStatus(isSuccess ? "성공" : "실패");
//        payment.setPaymentDate(paymentRequestDto.getPaymentDate());
//        payment.setPaymentMethod(paymentRequestDto.getPaymentMethod());
//
//        paymentRepository.save(payment);
//
//        // 결과 로깅
//        log.info("Payment {} with ID: {}", isSuccess ? "succeeded" : "failed", payment.getId());
//
//        return isSuccess;
//    }

    // 결제 진입 검증 로직
    public boolean validatePaymentEntry(PaymentRequestDto paymentRequestDto) {
        return "결제 진행 중".equals(paymentRequestDto.getPaymentStatus());
    }

    // 결제 처리 로직
    public boolean processPayment(PaymentRequestDto requestDto) {
        try {
            // 20% 실패 시뮬레이션
            boolean isSuccess = new Random().nextInt(100) >= 20;

            if (isSuccess) {
                // 결제 성공 처리
                log.info("Payment succeeded for product ID: {}", requestDto.getProductId());
                return true;
            } else {
                // 결제 실패 처리: 재고 복구 요청
                log.info("Payment failed for product ID: {}. Restoring stock...", requestDto.getProductId());
                productServiceFeignClient.restoreStock(requestDto.getProductId(), requestDto.getQuantity());
                return false;
            }
        } catch (Exception e) {
            log.error("Error occurred during payment processing: ", e);
            throw new RuntimeException("결제 처리 중 오류 발생", e);
        }
    }
}
