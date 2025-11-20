package org.example.cartservice.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.cartservice.entity.Cart;
import org.example.cartservice.entity.CartItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter
public class CartResponseDto {

    private Long cartId;
    private Long userId;
    private List<CartItemDto> items;
    private int totalAmount;
    private int totalItems;
    private LocalDateTime updatedAt;

    public CartResponseDto(Cart cart) {
        this.cartId = cart.getId();
        this.userId = cart.getUserId();
        this.items = cart.getCartItems().stream()
                .map(CartItemDto::new)
                .collect(Collectors.toList());
        this.totalAmount = cart.getTotalAmount();
        this.totalItems = cart.getTotalItems();
        this.updatedAt = cart.getUpdatedAt();
    }

    @Getter @Setter
    public static class CartItemDto {
        private Long cartItemId;
        private Long productId;
        private String productTitle;
        private Integer price;
        private Integer quantity;
        private Integer subtotal;
        private LocalDateTime addedAt;

        public CartItemDto(CartItem item) {
            this.cartItemId = item.getId();
            this.productId = item.getProductId();
            this.productTitle = item.getProductTitle();
            this.price = item.getPrice();
            this.quantity = item.getQuantity();
            this.subtotal = item.getPrice() * item.getQuantity();
            this.addedAt = item.getAddedAt();
        }
    }
}