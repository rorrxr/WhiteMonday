package com.minju.whitemonday.controller;

import com.minju.whitemonday.dto.OrderRequestDto;
import com.minju.whitemonday.dto.OrderResponseDto;
import com.minju.whitemonday.security.UserDetailsImpl;
import com.minju.whitemonday.service.OrderService;
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
    @PutMapping("/{orderId}/return")
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

}
