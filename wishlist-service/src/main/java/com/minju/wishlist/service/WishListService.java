package com.minju.wishlist.service;

import com.minju.wishlist.dto.WishListCreateRequestDto;
import com.minju.wishlist.dto.WishListResponseDto;
import com.minju.wishlist.dto.WishListUpdateRequestDto;
import com.minju.wishlist.entity.WishList;
import com.minju.wishlist.repository.WishListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minju.common.dto.ProductDto;
import com.minju.wishlist.client.ProductServiceClient;

@Service
@RequiredArgsConstructor
public class WishListService {

    private final WishListRepository wishListRepository;
    private final ProductServiceClient productServiceClient;

    // 위시리스트 조회
    @Transactional(readOnly = true)
    public WishListResponseDto wishList(Long userId) {
        WishList wishList = wishListRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("위시리스트가 존재하지 않습니다."));

        // 상품 정보 가져오기
        ProductDto product = productServiceClient.getProductById(wishList.getProductId());

        return new WishListResponseDto(wishList, product);
    }

    // 위시리스트 추가
    @Transactional
    public WishListResponseDto addWishList(WishListCreateRequestDto requestDto, Long userId) {
        // 상품 검증
        ProductDto product = productServiceClient.getProductById(requestDto.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("상품이 존재하지 않습니다.");
        }

        WishList wishList = wishListRepository.findByUserIdAndProductId(userId, requestDto.getProductId())
                .map(existingWishList -> {
                    // 이미 존재하는 경우 수량 업데이트
                    existingWishList.setQuantity(existingWishList.getQuantity() + requestDto.getQuantity());
                    return existingWishList;
                })
                .orElseGet(() -> WishList.builder()
                        .userId(userId)
                        .productId(requestDto.getProductId())
                        .quantity(requestDto.getQuantity())
                        .build());

        // 위시리스트 저장
        WishList savedWishList = wishListRepository.save(wishList);

        return new WishListResponseDto(savedWishList, product);
    }

    // 위시리스트 수정
    @Transactional
    public WishListResponseDto updateWishList(Long id, WishListUpdateRequestDto requestDto, Long userId) {
        WishList wishList = wishListRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("수정할 위시리스트가 존재하지 않습니다."));

        ProductDto product = productServiceClient.getProductById(wishList.getProductId());

        // 수량 업데이트
        wishList.setQuantity(requestDto.getQuantity());
        WishList updatedWishList = wishListRepository.save(wishList);

        return new WishListResponseDto(updatedWishList, product);
    }

    // 위시리스트 삭제
    @Transactional
    public void deleteWishListItem(Long id, Long userId) {
        WishList wishList = wishListRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 위시리스트가 존재하지 않습니다."));

        // 항목 삭제
        wishListRepository.delete(wishList);
    }
}
