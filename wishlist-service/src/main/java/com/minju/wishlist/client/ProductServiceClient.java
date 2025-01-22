package com.minju.wishlist.client;

import com.minju.common.dto.ProductDto;
import com.minju.wishlist.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "http://localhost:8082")
public interface ProductServiceClient {

    // 상품 상세 조회
    @GetMapping("/api/products/{productId}")
    ProductDto getProductById(@PathVariable("productId") Long productId);
}
