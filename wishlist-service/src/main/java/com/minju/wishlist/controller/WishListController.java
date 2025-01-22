package com.minju.wishlist.controller;

import com.minju.wishlist.dto.WishListCreateRequestDto;
import com.minju.wishlist.dto.WishListResponseDto;
import com.minju.wishlist.dto.WishListUpdateRequestDto;
import com.minju.wishlist.service.WishListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
public class WishListController {

    private final WishListService wishListService;

    // 위시리스트 조회
    @GetMapping
    public ResponseEntity<WishListResponseDto> getWishList(@RequestHeader("X-User-Id") Long userId) {
        WishListResponseDto wishList = wishListService.wishList(userId);
        return ResponseEntity.ok(wishList);
    }

    // 위시리스트 추가
    @PostMapping
    public ResponseEntity<WishListResponseDto> addWishList(
            @Valid @RequestBody WishListCreateRequestDto requestDto,
            @RequestHeader("X-User-Id") Long userId) {
        WishListResponseDto createdWishList = wishListService.addWishList(requestDto, userId);
        return ResponseEntity.status(201).body(createdWishList);
    }

    // 위시리스트 수정
    @PutMapping("/{id}")
    public ResponseEntity<WishListResponseDto> updateWishList(
            @PathVariable Long id,
            @Valid @RequestBody WishListUpdateRequestDto requestDto,
            @RequestHeader("X-User-Id") Long userId) {
        WishListResponseDto updatedWishList = wishListService.updateWishList(id, requestDto, userId);
        return ResponseEntity.ok(updatedWishList);
    }

    // 위시리스트 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWishList(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        wishListService.deleteWishListItem(id, userId);
        return ResponseEntity.noContent().build();
    }
}
