package com.minju.whitemonday.dto;

import com.minju.whitemonday.entity.WishList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WishListResponseDto {
    private Long id;
    private Long productId;
    private int quantity;
    private String productDetailUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WishListResponseDto(WishList wishlist) {
        this.id = wishlist.getId();
        this.productId = wishlist.getProduct().getId();
        this.quantity = wishlist.getQuantity();
        this.createdAt = wishlist.getCreatedAt();
        this.updatedAt = wishlist.getUpdatedAt();
        this.productDetailUrl = "/api/products/" + wishlist.getProduct().getId();
    }
}