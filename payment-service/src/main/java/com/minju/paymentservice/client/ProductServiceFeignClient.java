package com.minju.paymentservice.client;

import com.minju.common.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", configuration = FeignConfig.class)
public interface ProductServiceFeignClient {

    @PostMapping("/api/products/{id}/restore-stock")
    void restoreStock(@PathVariable("id") Long productId, @RequestParam("count") int count);

    @GetMapping("/api/products/{id}/remaining-stock")
    Integer getRemainingStock(@PathVariable("id") Long productId);
}