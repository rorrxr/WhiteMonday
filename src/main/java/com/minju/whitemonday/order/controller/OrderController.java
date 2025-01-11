package com.minju.whitemonday.order.controller;

import com.minju.whitemonday.order.dto.OrderRequestDto;
import com.minju.whitemonday.order.dto.OrderResponseDto;
import com.minju.whitemonday.order.entity.Order;
import com.minju.whitemonday.order.repository.OrderRepository;
import com.minju.whitemonday.user.service.UserDetailsImpl;
import com.minju.whitemonday.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    // 주문 생성
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @RequestBody OrderRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        OrderResponseDto order = orderService.createOrder(requestDto, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    // 주문 내역 조회
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getOrders(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<OrderResponseDto> orders = orderService.getOrders(userDetails.getUser());
        return ResponseEntity.ok(orders);
    }

    // 주문 취소
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        orderService.cancelOrder(orderId, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // 반품 처리
    @PutMapping("/return/{orderId}")
    public ResponseEntity<Void> returnOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        orderService.returnOrder(orderId, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // 주문 상태 업데이트 (관리자용)
    @PutMapping("/update-status")
    public ResponseEntity<Void> updateOrderStatus() {
        orderService.updateOrderStatus();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/payment/{orderId}")
    public ResponseEntity<Void> initiatePayment(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        // 결제 프로세스 시작 (예: PG사 결제 요청)
        // 결제 시도 시 처리 로직 작성 필요

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // 결제 API
    @PostMapping("/payment/complete/{orderId}/")
    public ResponseEntity<OrderResponseDto> completePayment(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        // 결제 진행 (결제 성공/실패 처리)
        // 고객의 귀책 이탈 시뮬레이션: 결제 중 20% 확률로 실패 처리
        if (Math.random() < 0.2) {  // 결제 실패 확률 20%
            order.setOrderStatus("PAYMENT_FAILED");
        } else {
            order.setOrderStatus("PAYMENT_COMPLETED");
        }

        orderRepository.save(order);
        return ResponseEntity.ok(new OrderResponseDto(order));
    }
    // 주문 정보 조회 API
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        return ResponseEntity.ok(new OrderResponseDto(order));
    }
}
