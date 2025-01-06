package com.minju.gatewayservice.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @PostMapping("/api/products")
    ResponseEntity<ProductResponseDto> addProduct(@RequestBody ProductRequestDto requestDto);
}

@Service
public class ApiGatewayService {

    private final ProductServiceClient productServiceClient;

    @Autowired
    public ApiGatewayService(ProductServiceClient productServiceClient) {
        this.productServiceClient = productServiceClient;
    }

    public ProductResponseDto addProductToService(ProductRequestDto requestDto) {
        return productServiceClient.addProduct(requestDto).getBody();
    }
}

