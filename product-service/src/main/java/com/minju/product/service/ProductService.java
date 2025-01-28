package com.minju.product.service;

import com.minju.product.dto.ProductRequestDto;
import com.minju.product.dto.ProductResponseDto;
import com.minju.product.entity.Product;
import com.minju.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Integer> redisTemplate;
    private final RedissonClient redissonClient;

    private static final String PRODUCT_KEY_PREFIX = "product:stock:";

    // 상품 등록
    public ProductResponseDto addProduct(ProductRequestDto requestDto) {
        Product product = new Product(
                requestDto.getId(),
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

        // 선착순 구매 상품일 경우 구매 가능 시간 검증
        if (product.isFlashSale() && !isFlashSaleAvailable(product)) {
            throw new IllegalArgumentException("현재 해당 상품은 구매할 수 없습니다. 구매 가능 시간: " + product.getFlashSaleStartTime());
        }

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

    // 선착순 구매 상품의 구매 가능 시간 검증
    private boolean isFlashSaleAvailable(Product product) {
        if (product.getFlashSaleStartTime() == null || product.getFlashSaleStartTime().isEmpty()) {
            return true; // 시간 제한이 없는 경우 구매 가능
        }

        LocalTime now = LocalTime.now();
        LocalTime flashSaleStartTime = LocalTime.parse(product.getFlashSaleStartTime()); // String → LocalTime 변환

        return now.isAfter(flashSaleStartTime);
    }

    // 스케줄링 작업: 선착순 상품 만료 처리 (5분마다 실행)
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    @Transactional
    public void processFlashSaleTimeouts() {
        log.info("스케줄링 작업 시작: 선착순 상품 및 만료 처리");

        List<Product> flashSaleProducts = productRepository.findByFlashSaleTrue();

        for (Product product : flashSaleProducts) {
            if (!isFlashSaleAvailable(product)) {
                log.info("선착순 상품 만료 처리: {}", product.getTitle());
                product.setFlashSale(false); // 만료된 상품 처리
                productRepository.save(product);
            }
        }

        log.info("스케줄링 작업 완료");
    }

    // Redis에서 정확한 실시간 재고 조회
    public int getAccurateStock(Long productId) {
        String redisKey = PRODUCT_KEY_PREFIX + productId;
        Integer stock = redisTemplate.opsForValue().get(redisKey);

        if (stock == null) {
            Product product = productRepository.findById(productId)
                    .orElse(null); // ➜ null을 반환하여 예외 방지

            if (product == null) {
                log.error("상품 ID {}가 존재하지 않음", productId);
                return -1; // 상품 없음
            }

            stock = product.getStock();
            redisTemplate.opsForValue().set(redisKey, stock);
        }
        return stock;
    }

    // 재고 감소 (트랜잭션 처리)
//    @Transactional
//    public void decreaseStockWithTransaction(Long productId, int count) {
//        String redisKey = PRODUCT_KEY_PREFIX + productId;
//        RLock lock = redissonClient.getLock("lock:" + redisKey);
//
//        try {
//            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) { // 10초 대기, 5초 락 유지
//                Integer currentStock = redisTemplate.opsForValue().get(redisKey);
//                if (currentStock == null || currentStock < count) {
//                    throw new IllegalArgumentException("재고가 부족합니다.");
//                }
//
//                // Redis 재고 감소
//                redisTemplate.opsForValue().decrement(redisKey, count);
//
//                // DB 재고 동기화
//                Product product = productRepository.findById(productId)
//                        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
//                product.setStock(product.getStock() - count);
//                productRepository.save(product);
//            } else {
//                throw new IllegalStateException("동시 요청 과부하로 인해 처리할 수 없습니다.");
//            }
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new RuntimeException("재고 감소 중 오류가 발생했습니다.", e);
//        } finally {
//            lock.unlock();
//        }
//    }

    // 재고 감소 (트랜잭션 처리)
    @Transactional
    public void decreaseStockWithTransaction(Long productId, int count) {
        RLock lock = redissonClient.getLock(PRODUCT_KEY_PREFIX + productId);

        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                Integer currentStock = redisTemplate.opsForValue().get(PRODUCT_KEY_PREFIX + productId);
                if (currentStock == null || currentStock < count) {
                    throw new IllegalArgumentException("재고가 부족합니다.");
                }
                redisTemplate.opsForValue().decrement(PRODUCT_KEY_PREFIX + productId, (long) count);

                // 데이터베이스 업데이트
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
                product.setStock(product.getStock() - count);
                productRepository.save(product);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Lock 획득 중 오류가 발생했습니다.", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock(); // 락 해제
            }
        }
    }

    // 재고 복구 (트랜잭션 처리)
//    @Transactional
//    public void restoreStock(Long productId, int count) {
//        String redisKey = PRODUCT_KEY_PREFIX + productId;
//        RLock lock = redissonClient.getLock("lock:" + redisKey);
//
//        try {
//            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) { // 10초 대기, 5초 락 유지
//                // Redis 재고 증가
//                redisTemplate.opsForValue().increment(redisKey, count);
//
//                // DB 재고 동기화
//                Product product = productRepository.findById(productId)
//                        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
//                product.setStock(product.getStock() + count);
//                productRepository.save(product);
//            } else {
//                throw new IllegalStateException("동시 요청 과부하로 인해 처리할 수 없습니다.");
//            }
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new RuntimeException("재고 복구 중 오류가 발생했습니다.", e);
//        } finally {
//            lock.unlock();
//        }
//    }

    // 재고 복구 (트랜잭션 처리)
    @Transactional
    public void restoreStock(Long productId, int count) {
        String redisKey = PRODUCT_KEY_PREFIX + productId;
        RLock lock = redissonClient.getLock("lock:" + redisKey);

        try {
            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) { // Lock 획득
                redisTemplate.opsForValue().increment(redisKey, count); // Redis 재고 증가

                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
                product.setStock(product.getStock() + count); // DB 재고 동기화
                productRepository.save(product);
            } else {
                throw new IllegalStateException("동시 요청 과부하로 인해 처리할 수 없습니다.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock 획득 중 오류가 발생했습니다.", e);
        } finally {
            if (lock.isHeldByCurrentThread()) { // Null 및 소유 여부 체크
                lock.unlock();
            }
        }
    }


    // Redis에서 재고 조회
    public int getStock(Long productId) {
        String redisKey = PRODUCT_KEY_PREFIX + productId;
        Integer stock = redisTemplate.opsForValue().get(redisKey);

        if (stock == null) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
            stock = product.getStock();
            redisTemplate.opsForValue().set(redisKey, stock); // Redis 캐싱
        }
        return stock;
    }
}