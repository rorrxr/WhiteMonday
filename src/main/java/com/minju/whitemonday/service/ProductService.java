package com.minju.whitemonday.service;

import com.minju.whitemonday.dto.ProductRequestDto;
import com.minju.whitemonday.dto.ProductResponseDto;
import com.minju.whitemonday.entity.Product;
import com.minju.whitemonday.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    // 상품 등록
    public ProductResponseDto addProduct(ProductRequestDto requestDto) {
        // DTO를 엔터티로 변환
        Product product = new Product(
                requestDto.getTitle(),
                requestDto.getDescription(),
                requestDto.getPrice(),
                requestDto.getStock(),
                requestDto.isFlashSale(),
                requestDto.getFlashSaleStartTime()
        );

        // 엔터티 저장
        Product savedProduct = productRepository.save(product);

        // 저장된 엔터티를 응답 DTO로 변환
        return new ProductResponseDto(
                savedProduct.getProductId(),
                savedProduct.getTitle(),
                savedProduct.getDescription(),
                savedProduct.getPrice(),
                savedProduct.getStock(),
                savedProduct.isFlashSale(),
                savedProduct.getFlashSaleStartTime(),
                savedProduct.getCreatedAt(),
                savedProduct.getUpdatedAt()
        );
    }

    // 상품 리스트 조회
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(product -> new ProductResponseDto(
                        product.getProductId(),
                        product.getTitle(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getStock(),
                        product.isFlashSale(),
                        product.getFlashSaleStartTime(),
                        product.getCreatedAt(),
                        product.getUpdatedAt()
                ))
                .toList();
    }

    // 상품 상세 조회
    public ProductResponseDto getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        return new ProductResponseDto(
                product.getProductId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.isFlashSale(),
                product.getFlashSaleStartTime(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}

