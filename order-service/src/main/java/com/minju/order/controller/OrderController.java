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
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * 장바구니에서 주문 생성
     */
    @PostMapping("/from-cart")
    public ResponseEntity<OrderResponseDto> createOrderFromCart(
            @RequestHeader("X-User-Id") Long userId) {

        OrderResponseDto response = orderService.createOrderFromCart(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 주문 내역 조회
     */
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getOrders(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(orderService.getOrders(userId));
    }

    /**
     * 주문 취소
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, userId));
    }

    /**
     * 반품
     */
    @PutMapping("/{orderId}/return")
    public ResponseEntity<OrderResponseDto> returnOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(orderService.returnOrder(orderId, userId));
    }

    /**
     * 상태 업데이트 (스케줄러)
     */
    @PutMapping("/update-status")
    public ResponseEntity<Void> updateOrderStatus() {
        orderService.updateOrderStatus();
        return ResponseEntity.noContent().build();
    }
}
