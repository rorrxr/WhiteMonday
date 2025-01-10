package com.minju.wishlist.dto;

import com.minju.common.dto.ProductDto;
import com.minju.wishlist.entity.WishList;
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

    // 기존 생성자에서 ProductServiceClient를 통해 상품 정보를 조회
    public WishListResponseDto(WishList wishlist, ProductDto product) {
        this.id = wishlist.getId();
        this.productId = wishlist.getProductId(); // Directly use productId from wishlist
        this.quantity = wishlist.getQuantity();
        this.createdAt = wishlist.getCreatedAt();
        this.updatedAt = wishlist.getUpdatedAt();
        this.productDetailUrl = "/api/products/" + product.getProductId(); // Create productDetailUrl based on product info
    }
}