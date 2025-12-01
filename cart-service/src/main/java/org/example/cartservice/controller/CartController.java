package org.example.cartservice.controller;

import com.minju.common.dto.CommonResponse;
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
    public ResponseEntity<CommonResponse<CartResponseDto>> addToCart(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid AddToCartRequestDto requestDto) {

        CartResponseDto response = cartService.addToCart(userId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        HttpStatus.CREATED.value(),
                        "장바구니에 상품이 추가되었습니다.",
                        response
                ));
    }

    /**
     * 장바구니 조회
     */
    @GetMapping
    public ResponseEntity<CommonResponse<CartResponseDto>> getCart(
            @RequestHeader("X-User-Id") Long userId) {

        CartResponseDto cart = cartService.getCart(userId);

        return ResponseEntity.ok(
                CommonResponse.success("장바구니 조회에 성공했습니다.", cart)
        );
    }

    /**
     * 장바구니 아이템 삭제
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CommonResponse<CartResponseDto>> removeFromCart(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long cartItemId) {

        CartResponseDto cart = cartService.removeFromCart(userId, cartItemId);

        return ResponseEntity.ok(
                CommonResponse.success("장바구니에서 상품이 삭제되었습니다.", cart)
        );
    }

    /**
     * 장바구니 수량 변경
     */
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CommonResponse<CartResponseDto>> updateQuantity(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity) {

        CartResponseDto cart = cartService.updateQuantity(userId, cartItemId, quantity);

        return ResponseEntity.ok(
                CommonResponse.success("장바구니 상품 수량이 변경되었습니다.", cart)
        );
    }

    /**
     * 장바구니 전체 비우기
     */
    @DeleteMapping
    public ResponseEntity<CommonResponse<Void>> clearCart(
            @RequestHeader("X-User-Id") Long userId) {

        cartService.clearCart(userId);

        return ResponseEntity.ok(
                CommonResponse.success("장바구니가 비워졌습니다.", null)
        );
    }
}
