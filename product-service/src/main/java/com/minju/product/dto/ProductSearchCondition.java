package com.minju.product.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductSearchCondition {
    private String keyword;        // 상품명 검색
    private Integer minPrice;      // 최소 가격
    private Integer maxPrice;      // 최대 가격
    private Boolean flashSale;     // 플래시 세일 여부
    private Boolean inStock;       // 재고 있는 상품만
}
