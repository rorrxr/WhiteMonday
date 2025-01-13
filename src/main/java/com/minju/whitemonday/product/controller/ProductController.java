package com.minju.whitemonday.product.controller;

import com.minju.whitemonday.product.dto.ProductDto;
import com.minju.whitemonday.product.dto.ProductRequestDto;
import com.minju.whitemonday.product.dto.ProductResponseDto;
import com.minju.whitemonday.product.entity.Product;
import com.minju.whitemonday.product.repository.ProductRepository;
import com.minju.whitemonday.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;
    private final ProductRepository productRepository;

    // 상품 전체 조회하기
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        List<ProductResponseDto> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    // 상품 상세 조회하기
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        ProductDto productDto = new ProductDto();
        productDto.setProductId(product.getId());
        productDto.setTitle(product.getTitle());
        productDto.setDescription(product.getDescription());
        productDto.setPrice(product.getPrice());
        productDto.setStock(product.getStock());
        productDto.setFlashSale(product.isFlashSale());

        // 재고가 적을 경우 "재고 부족" 메시지 추가
        if (product.getStock() < 5) {
            productDto.setStockMessage("재고 부족");
        } else {
            productDto.setStockMessage("재고 있음");
        }

        productDto.setFlashSaleStartTime(product.getFlashSaleStartTime());

        return ResponseEntity.ok(productDto);
    }

    // 상품 등록
    @PostMapping
    public ResponseEntity<ProductResponseDto> addProduct(@RequestBody ProductRequestDto requestDto) {
        ProductResponseDto savedProduct = productService.addProduct(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }

    // 상품 남은 수량 API
    @GetMapping("/stock/{productId}")
    public ResponseEntity<Integer> getRemainingStock(@PathVariable Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 재고가 5개 미만일 경우 경고 메시지 추가
        if (product.getStock() < 5) {
            // 재고 부족에 대해 클라이언트에게 알림
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(product.getStock()); // 422 상태 코드
        }

        return ResponseEntity.ok(product.getStock());
    }

}
