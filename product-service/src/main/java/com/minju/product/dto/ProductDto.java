package com.minju.product.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
public class ProductDto {
    private Long productId; // 엔터티의 ID
    private String title;
    private String description;
    private int price;
    private int stock; // 재고 정보 반환 필요 시 포함
    private boolean isFlashSale;
    private String  flashSaleStartTime;
    private String  createdAt;
    private String  updatedAt;
}
