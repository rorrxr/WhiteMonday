package com.minju.common.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class CartResponseDto {

    private Long cartId;
    private Long userId;
    private List<CartItemDto> items;
    private int totalAmount;
    private int totalItems;
    private LocalDateTime updatedAt;

    @Getter @Setter
    public static class CartItemDto {
        private Long cartItemId;
        private Long productId;
        private String productTitle;
        private Integer price;
        private Integer quantity;
        private Integer subtotal;
        private LocalDateTime addedAt;
    }
}