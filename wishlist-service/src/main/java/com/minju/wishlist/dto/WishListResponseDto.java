package com.minju.wishlist.dto;

import com.minju.common.dto.ProductDto;
import com.minju.wishlist.entity.WishList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class WishListResponseDto {
    private Long id;
    private Long productId;
    private int quantity;
    private String productDetailUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WishListResponseDto(WishList wishList, ProductDto product) {
        this.id = wishList.getId();
        this.productId = wishList.getProductId();
        this.quantity = wishList.getQuantity();
        this.createdAt = wishList.getCreatedAt();
        this.updatedAt = wishList.getUpdatedAt();
        this.productDetailUrl = product != null ? "/api/products/" + product.getProductId() : null;
    }
}