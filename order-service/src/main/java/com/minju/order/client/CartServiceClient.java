package com.minju.order.client;

import com.minju.common.dto.CartResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "cart-service", url = "${cart.service.url:http://localhost:8084}")
public interface CartServiceClient {

    /**
     * 사용자의 장바구니 조회
     */
    @GetMapping("/api/cart")
    CartResponseDto getCart(@RequestHeader("X-User-Id") Long userId);

    /**
     * 장바구니 전체 비우기
     */
    @DeleteMapping("/api/cart")
    void clearCart(@RequestHeader("X-User-Id") Long userId);
}