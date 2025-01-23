package com.minju.wishlist.controller;

import com.minju.wishlist.dto.WishListCreateRequestDto;
import com.minju.wishlist.dto.WishListResponseDto;
import com.minju.wishlist.dto.WishListUpdateRequestDto;
import com.minju.wishlist.service.WishListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
public class WishListController {

    private final WishListService wishListService;

    // 위시리스트 추가 (POST /api/wishlist)
    @PostMapping
    public ResponseEntity<WishListResponseDto> addWishList(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid WishListCreateRequestDto requestDto) {
        WishListResponseDto response = wishListService.addWishList(requestDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 위시리스트 조회 (GET /api/wishlist)
    @GetMapping
    public ResponseEntity<List<WishListResponseDto>> getWishLists(@RequestHeader("X-User-Id") Long userId) {
        List<WishListResponseDto> wishLists = wishListService.getWishLists(userId);
        return ResponseEntity.ok(wishLists);
    }

    // 위시리스트 수정 (PUT /api/wishlist/{id})
    @PutMapping("/{id}")
    public ResponseEntity<WishListResponseDto> updateWishList(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid WishListUpdateRequestDto requestDto) {
        WishListResponseDto response = wishListService.updateWishList(id, requestDto, userId);
        return ResponseEntity.ok(response);
    }

    // 위시리스트 삭제 (DELETE /api/wishlist/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWishList(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        wishListService.deleteWishListItem(id, userId);
        return ResponseEntity.noContent().build();
    }
}