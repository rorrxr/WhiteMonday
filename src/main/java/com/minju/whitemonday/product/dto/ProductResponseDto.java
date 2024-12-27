package com.minju.whitemonday.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private Long productId; // 엔터티의 ID
    private String title;
    private String description;
    private int price;
    private int stock; // 재고 정보 반환 필요 시 포함
    private boolean isFlashSale;
    private LocalDateTime flashSaleStartTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
