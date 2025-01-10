package com.minju.wishlist.service;

import com.minju.common.dto.ProductDto;

import com.minju.wishlist.client.ProductServiceClient;
import com.minju.wishlist.dto.WishListCreateRequestDto;
import com.minju.wishlist.dto.WishListResponseDto;
import com.minju.wishlist.dto.WishListUpdateRequestDto;
import com.minju.wishlist.entity.WishList;
import com.minju.wishlist.repository.WishListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishListService {

    private final WishListRepository wishListRepository;
    ProductServiceClient productServiceClient;  // Feign Client 주입

    // 위시리스트 조회
    @Transactional(readOnly = true)
    public List<WishListResponseDto> getWishList(Long userId) {
        List<WishList> wishLists = wishListRepository.findAllByUserId(userId);

        // 상품 정보를 가져오기 위해 ProductServiceClient 사용
        return wishLists.stream()
                .map(wishList -> {
                    // ProductDto를 ProductServiceClient를 통해 가져옴
                    ProductDto product = productServiceClient.getProductById(wishList.getProductId());
                    return new WishListResponseDto(wishList, product);  // 상품 정보를 전달하여 DTO 생성
                })
                .collect(Collectors.toList());
    }

//     위시리스트에 상품 추가
    @Transactional
    public WishListResponseDto addToWishList(WishListCreateRequestDto requestDto, Long userId) {
        // 상품 존재 여부를 ProductService에서 확인
        ProductDto product = productServiceClient.getProductById(requestDto.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("상품이 존재하지 않습니다.");
        }

        WishList wishList = wishListRepository.findByUserIdAndProductId(userId, requestDto.getProductId())
                .orElse(new WishList(userId, requestDto.getProductId(), 0));

        wishList.setQuantity(wishList.getQuantity() + requestDto.getQuantity());
        WishList savedWishList = wishListRepository.save(wishList);
        return new WishListResponseDto(savedWishList, product);  // 상품 정보를 전달하여 DTO 생성
    }

//     위시리스트 항목 수정
    @Transactional
    public WishListResponseDto updateWishList(Long id, WishListUpdateRequestDto requestDto, Long userId) {
        WishList wishList = wishListRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 위시리스트는 본인의 것이 아닙니다."));

        wishList.updateQuantity(requestDto.getQuantity());
        ProductDto product = productServiceClient.getProductById(wishList.getProductId());
        return new WishListResponseDto(wishList, product);  // 상품 정보를 전달하여 DTO 생성
    }

    // 위시리스트 항목 삭제
    @Transactional
    public void deleteFromWishList(Long id, Long userId) {
        WishList wishList = wishListRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 위시리스트는 본인의 것이 아닙니다."));

        wishListRepository.delete(wishList);
    }
}