package org.example.cartservice.service;

import com.minju.common.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cartservice.client.ProductServiceClient;
import org.example.cartservice.dto.AddToCartRequestDto;
import org.example.cartservice.dto.CartResponseDto;
import org.example.cartservice.entity.Cart;
import org.example.cartservice.entity.CartItem;
import org.example.cartservice.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final ProductServiceClient productServiceClient;

    /**
     * 장바구니에 상품 추가 (동기 처리 - 즉시 응답 필요)
     */
    @Transactional
    public CartResponseDto addToCart(Long userId, AddToCartRequestDto requestDto) {
        log.info("장바구니 추가 시작 - userId: {}, productId: {}", userId, requestDto.getProductId());

        // 1. 상품 정보 조회 및 검증 (동기 - Feign Client)
        ProductDto product = productServiceClient.getProductById(requestDto.getProductId());

        if (product.getStock() < requestDto.getQuantity()) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + product.getStock());
        }

        // 2. 장바구니 조회 또는 생성
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createNewCart(userId));

        // 3. 동일 상품 확인 및 수량 업데이트 or 신규 추가
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(requestDto.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // 기존 아이템 수량 증가
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + requestDto.getQuantity();

            if (product.getStock() < newQuantity) {
                throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + product.getStock());
            }

            item.setQuantity(newQuantity);
            log.info("장바구니 수량 업데이트 - cartItemId: {}, newQuantity: {}", item.getId(), newQuantity);
        } else {
            // 새 아이템 추가
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(product.getProductId());
            newItem.setProductTitle(product.getTitle());
            newItem.setProductDescription(product.getDescription());
            newItem.setPrice(product.getPrice());
            newItem.setQuantity(requestDto.getQuantity());

            cart.getCartItems().add(newItem);
            log.info("장바구니 신규 아이템 추가 - productId: {}", product.getProductId());
        }

        Cart savedCart = cartRepository.save(cart);
        return new CartResponseDto(savedCart);
    }

    /**
     * 장바구니 조회
     */
    @Transactional(readOnly = true)
    public CartResponseDto getCart(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 비어있습니다."));

        return new CartResponseDto(cart);
    }

    /**
     * 장바구니 아이템 삭제
     */
    @Transactional
    public CartResponseDto removeFromCart(Long userId, Long cartItemId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니를 찾을 수 없습니다."));

        boolean removed = cart.getCartItems().removeIf(item -> item.getId().equals(cartItemId));

        if (!removed) {
            throw new IllegalArgumentException("해당 상품이 장바구니에 없습니다.");
        }

        Cart savedCart = cartRepository.save(cart);
        log.info("장바구니 아이템 삭제 - cartItemId: {}", cartItemId);

        return new CartResponseDto(savedCart);
    }

    /**
     * 장바구니 수량 변경
     */
    @Transactional
    public CartResponseDto updateQuantity(Long userId, Long cartItemId, Integer newQuantity) {
        if (newQuantity < 1) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니를 찾을 수 없습니다."));

        CartItem item = cart.getCartItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 장바구니에 없습니다."));

        // 재고 확인
        ProductDto product = productServiceClient.getProductById(item.getProductId());
        if (product.getStock() < newQuantity) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + product.getStock());
        }

        item.setQuantity(newQuantity);
        Cart savedCart = cartRepository.save(cart);
        log.info("장바구니 수량 변경 - cartItemId: {}, newQuantity: {}", cartItemId, newQuantity);

        return new CartResponseDto(savedCart);
    }

    /**
     * 장바구니 전체 비우기
     */
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니를 찾을 수 없습니다."));

        cart.getCartItems().clear();
        cartRepository.save(cart);
        log.info("장바구니 전체 삭제 - userId: {}", userId);
    }

    private Cart createNewCart(Long userId) {
        Cart newCart = new Cart();
        newCart.setUserId(userId);
        return cartRepository.save(newCart);
    }
}