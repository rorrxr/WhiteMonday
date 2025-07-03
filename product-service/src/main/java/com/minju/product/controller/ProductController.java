package com.minju.product.controller;

import com.minju.common.dto.StockResponse;
import com.minju.product.dto.DecreaseStockRequest;
import com.minju.product.dto.ProductRequestDto;
import com.minju.product.dto.ProductResponseDto;
import com.minju.product.service.ProductService;
import com.minju.product.service.StockService;
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
    private final StockService stockService;

    // 상품 전체 조회
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        List<ProductResponseDto> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    // 상품 상세 조회 (선착순 구매 상품 구분)
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long productId) {
        ProductResponseDto product = productService.getProductById(productId);
        return ResponseEntity.ok(product);
    }

    // 스케줄링 테스트 API (선착순 상품 만료 처리)
    @GetMapping("/scheduled")
    public ResponseEntity<String> triggerScheduledTask() {
        productService.processFlashSaleTimeouts();
        return ResponseEntity.ok("스케줄링 작업이 수동으로 실행되었습니다.");
    }

    // 상품 등록
    @PostMapping
    public ResponseEntity<ProductResponseDto> addProduct(@RequestBody ProductRequestDto requestDto) {
        ProductResponseDto savedProduct = productService.addProduct(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }

    // 상품 남은 재고 조회 (Redis)
    @GetMapping("/{id}/remaining-stock")
    public ResponseEntity<Integer> getRemainingStock(@PathVariable Long id) {
        int remainingStock = productService.getAccurateStock(id);
        return ResponseEntity.ok(remainingStock);
    }

    // 재고 복구 (트랜잭션 처리)
    @PostMapping("/{id}/restore-stock")
    public ResponseEntity<Void> restoreStock(@PathVariable Long id, @RequestParam int count) {
        stockService.restoreStock(id, count);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // 재고 감소
    @PostMapping("/{productId}/stock/decrease")
    public ResponseEntity<StockResponse> decreaseStock(
            @PathVariable Long productId,
            @RequestBody DecreaseStockRequest request
    ) {
        return ResponseEntity.ok(stockService.decreaseStock(productId, request.getQuantity()));
    }
}
