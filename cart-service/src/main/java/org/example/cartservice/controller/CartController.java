package org.example.cartservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.cartservice.dto.AddToCartRequestDto;
import org.example.cartservice.dto.CartResponseDto;
import org.example.cartservice.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    /**
     * 장바구니에 상품 추가
     */
    @PostMapping
    public ResponseEntity<CartResponseDto> addToCart(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid AddToCartRequestDto requestDto) {

        CartResponseDto response = cartService.addToCart(userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 장바구니 조회
     */
    @GetMapping
    public ResponseEntity<CartResponseDto> getCart(
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(cartService.getCart(userId));
    }

    /**
     * 장바구니 아이템 삭제
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponseDto> removeFromCart(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long cartItemId) {

        return ResponseEntity.ok(cartService.removeFromCart(userId, cartItemId));
    }

    /**
     * 장바구니 수량 변경
     */
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponseDto> updateQuantity(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity) {

        return ResponseEntity.ok(cartService.updateQuantity(userId, cartItemId, quantity));
    }

    /**
     * 장바구니 전체 비우기
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @RequestHeader("X-User-Id") Long userId) {

        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}