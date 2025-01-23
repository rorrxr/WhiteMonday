package com.minju.order.controller;

import com.minju.order.dto.OrderRequestDto;
import com.minju.order.dto.OrderResponseDto;
import com.minju.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid OrderRequestDto requestDto) {
        OrderResponseDto response = orderService.createOrder(userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 주문 내역 조회
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getOrders(@RequestHeader("X-User-Id") Long userId) {
        List<OrderResponseDto> orders = orderService.getOrders(userId);
        return ResponseEntity.ok(orders);
    }

    // 주문 취소
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {
        OrderResponseDto response = orderService.cancelOrder(orderId, userId);
        return ResponseEntity.ok(response);
    }

    // 반품 처리
    @PutMapping("/{orderId}/return")
    public ResponseEntity<OrderResponseDto> returnOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {
        OrderResponseDto response = orderService.returnOrder(orderId, userId);
        return ResponseEntity.ok(response);
    }

    // 주문 상태 업데이트 (관리자용)
    @PutMapping("/update-status")
    public ResponseEntity<Void> updateOrderStatus() {
        orderService.updateOrderStatus();
        return ResponseEntity.noContent().build();
    }
}