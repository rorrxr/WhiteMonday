package com.minju.product.service;

import com.minju.product.dto.ProductRequestDto;
import com.minju.product.dto.ProductResponseDto;
import com.minju.product.entity.Product;
import com.minju.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Integer> redisTemplate;
    private final RedissonClient redissonClient;

    private static final String PRODUCT_KEY_PREFIX = "product:stock:";

    // 상품 등록
    public ProductResponseDto addProduct(ProductRequestDto requestDto) {
        Product product = new Product(
                requestDto.getTitle(),
                requestDto.getDescription(),
                requestDto.getPrice(),
                requestDto.getStock(),
                requestDto.isFlashSale(),
                requestDto.getFlashSaleStartTime()
        );

        Product savedProduct = productRepository.save(product);

        // Redis에 초기 재고 저장
        redisTemplate.opsForValue().set(PRODUCT_KEY_PREFIX + savedProduct.getId(), savedProduct.getStock());

        return new ProductResponseDto(
                savedProduct.getId(),
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
                        product.getId(),
                        product.getTitle(),
                        product.getDescription(),
                        product.getPrice(),
                        getStock(product.getId()), // Redis에서 재고 조회
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
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                getStock(product.getId()), // Redis에서 재고 조회
                product.isFlashSale(),
                product.getFlashSaleStartTime(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    // 재고 감소
    public void decreaseStock(Long productId, int count) {
        String redisKey = PRODUCT_KEY_PREFIX + productId;
        RLock lock = redissonClient.getLock("lock:" + redisKey); // 동시성 제어를 위한 락

        lock.lock();
        try {
            Integer currentStock = getStock(productId); // Redis에서 재고 조회

            if (currentStock < count) {
                throw new IllegalArgumentException("재고가 부족합니다.");
            }

            // Redis에서 재고 감소
            redisTemplate.opsForValue().set(redisKey, currentStock - count);

        } finally {
            lock.unlock();
        }
    }

    // 재고 증가
    public void increaseStock(Long productId, int count) {
        String redisKey = PRODUCT_KEY_PREFIX + productId;

        // Redis에서 현재 재고 조회
        Integer currentStock = getStock(productId);

        // Redis에서 재고 증가
        redisTemplate.opsForValue().set(redisKey, currentStock + count);

        // 데이터베이스에도 반영
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        product.setStock(product.getStock() + count);
        productRepository.save(product);
    }

    // Redis에서 재고 조회
    public int getStock(Long productId) {
        String redisKey = PRODUCT_KEY_PREFIX + productId;

        // Redis에서 재고 조회
        Integer stock = redisTemplate.opsForValue().get(redisKey);

        if (stock == null) {
            // Redis에 캐싱이 없으면 DB에서 조회 후 캐싱
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
            stock = product.getStock();
            redisTemplate.opsForValue().set(redisKey, stock, Duration.ofMinutes(10)); // 캐싱 시간 10분
        }

        return stock;
    }
}
