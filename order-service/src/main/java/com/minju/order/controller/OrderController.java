package com.minju.order.controller;

import com.minju.common.dto.CommonResponse;
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

    /**
     * 장바구니에서 주문 생성
     */
    @PostMapping("/from-cart")
    public ResponseEntity<CommonResponse<OrderResponseDto>> createOrderFromCart(
            @RequestHeader("X-User-Id") Long userId) {

        OrderResponseDto response = orderService.createOrderFromCart(userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        HttpStatus.CREATED.value(),
                        "주문이 정상적으로 생성되었습니다.",
                        response
                ));
    }

    /**
     * 주문 내역 조회
     */
    @GetMapping
    public ResponseEntity<CommonResponse<List<OrderResponseDto>>> getOrders(
            @RequestHeader("X-User-Id") Long userId) {

        List<OrderResponseDto> orders = orderService.getOrders(userId);

        return ResponseEntity.ok(
                CommonResponse.success("주문 내역 조회에 성공했습니다.", orders)
        );
    }

    /**
     * 주문 취소
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<CommonResponse<OrderResponseDto>> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {

        OrderResponseDto result = orderService.cancelOrder(orderId, userId);

        return ResponseEntity.ok(
                CommonResponse.success("주문이 취소되었습니다.", result)
        );
    }

    /**
     * 반품
     */
    @PutMapping("/{orderId}/return")
    public ResponseEntity<CommonResponse<OrderResponseDto>> returnOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {

        OrderResponseDto result = orderService.returnOrder(orderId, userId);

        return ResponseEntity.ok(
                CommonResponse.success("주문이 반품 처리되었습니다.", result)
        );
    }

    /**
     * 상태 업데이트 (스케줄러)
     */
    @PutMapping("/update-status")
    public ResponseEntity<CommonResponse<Void>> updateOrderStatus() {
        orderService.updateOrderStatus();

        return ResponseEntity.ok(
                CommonResponse.success("주문 상태 업데이트 작업이 실행되었습니다.", null)
        );
    }
}
