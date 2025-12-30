package com.minju.product.repository;

import com.minju.product.dto.ProductSearchCondition;
import com.minju.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductRepositoryCustom {

    /**
     * 동적 상품 검색 (가격, 재고, 플래시세일, 키워드)
     */
    Page<Product> searchProducts(ProductSearchCondition condition, Pageable pageable);

    /**
     * 만료된 플래시세일 상품 조회 (시간 기반)
     */
    List<Product> findExpiredFlashSaleProducts(String currentTime);

    /**
     * 재고 있는 상품만 조회 (페이징)
     */
    Page<Product> findAvailableProducts(Pageable pageable);

    /**
     * 플래시세일 상품 조회 (페이징)
     */
    Page<Product> findFlashSaleProducts(Pageable pageable);
}
