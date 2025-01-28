package com.minju.product.controller;

import com.minju.common.dto.ProductDto;
import com.minju.product.dto.ProductRequestDto;
import com.minju.product.dto.ProductResponseDto;
import com.minju.product.entity.Product;
import com.minju.product.repository.ProductRepository;
import com.minju.product.service.ProductService;
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


    // 상품 상세 조회 (선착순 구매 상품 구분)
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long productId) {
        ProductResponseDto product = productService.getProductById(productId);
        return ResponseEntity.ok(product);
    }

    // 스케줄링 테스트 API
    @GetMapping("/scheduled")
    public ResponseEntity<String> triggerScheduledTask() {
        productService.processFlashSaleTimeouts();
        return ResponseEntity.ok("스케줄링 작업이 수동으로 실행되었습니다.");
    }

    // 상품 상세 조회하기
//    @GetMapping("/{id}")
//    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
//        ProductResponseDto product = productService.getProductById(id);
//        return ResponseEntity.ok(product);
//    }

    // 상품 등록
    @PostMapping
    public ResponseEntity<ProductResponseDto> addProduct(@RequestBody ProductRequestDto requestDto) {
        ProductResponseDto savedProduct = productService.addProduct(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }

    // 재고 감소
//    @PostMapping("/{id}/decrease-stock")
//    public ResponseEntity<Void> decreaseStock(
//            @PathVariable("id") Long productId,
//            @RequestParam("count") int count) {
//        productService.decreaseStock(productId, count);
//        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
//    }
//
//    // 재고 증가
//    @PostMapping("/{id}/increase-stock")
//    public ResponseEntity<Void> increaseStock(
//            @PathVariable("id") Long productId,
//            @RequestParam("count") int count) {
//        productService.increaseStock(productId, count);
//        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
//    }
//
//    // 상품 남은 수량 조회 (Redis)
//    @GetMapping("/{id}/remaining-stock")
//    public ResponseEntity<Integer> getRemainingStock(@PathVariable("id") Long productId) {
//        int remainingStock = productService.getStock(productId);
//        return ResponseEntity.ok(remainingStock);
//    }

    // 상품 남은 수량 조회 (직접 DB 조회)
//    @GetMapping("/{id}/remaining-stock")
//    public ResponseEntity<Integer> getRemainingStock(@PathVariable("id") Long productId) {
//        int remainingStock = productService.getRemainingStock(productId);
//        return ResponseEntity.ok(remainingStock);
//    }

    // 상품 남은 재고 조회
    @GetMapping("/{id}/remaining-stock")
    public ResponseEntity<Integer> getRemainingStock(@PathVariable Long id) {
        int remainingStock = productService.getAccurateStock(id);
        return ResponseEntity.ok(remainingStock);
    }

    // 재고 감소
    @PostMapping("/{id}/decrease-stock")
    public ResponseEntity<Void> decreaseStock(@PathVariable Long id, @RequestParam int count) {
        productService.decreaseStockWithTransaction(id, count);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // 재고 복구
    @PostMapping("/{id}/restore-stock")
    public ResponseEntity<Void> restoreStock(@PathVariable Long id, @RequestParam int count) {
        productService.restoreStock(id, count);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
