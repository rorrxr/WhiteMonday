package com.minju.order.client;

import com.minju.common.config.FeignConfig;
import com.minju.common.dto.ProductDto;
import com.minju.order.dto.StockRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service", configuration = FeignConfig.class)
public interface ProductServiceClient {

    @GetMapping("/api/products/{productId}")
    ProductDto getProductById(@PathVariable("productId") Long productId);


    @PostMapping("/api/products/{id}/decrease-stock")
    void decreaseStock(@PathVariable("id") Long productId, @RequestParam("count") int count);

    @PostMapping("/api/products/{id}/increase-stock")
    void increaseStock(@PathVariable("id") Long productId, @RequestParam("count") int count);

}