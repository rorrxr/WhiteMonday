package com.minju.wishlist.controller;

import com.minju.user.dto.UserInfoDto;
import com.minju.user.service.UserDetailsImpl;
import com.minju.wishlist.client.UserServiceClient;
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
    private final UserServiceClient userServiceClient;

    // 위시리스트 추가
    @PostMapping
    public ResponseEntity<WishListResponseDto> addToWishList(
            @Valid @RequestBody WishListCreateRequestDto requestDto,
            @RequestHeader("X-User-Id") String userId) {

        // User service to validate user info
        UserInfoDto userInfo = userServiceClient.getUserInfo(Long.valueOf(userId));

        if (userInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WishListResponseDto savedWishList = wishListService.addToWishList(requestDto, Long.valueOf(userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(savedWishList);
    }

    // 위시리스트 조회
    @GetMapping
    public ResponseEntity<List<WishListResponseDto>> getWishList(
            @RequestHeader("X-User-Id") String userId) {

        // User service to validate user info
        UserInfoDto userInfo = userServiceClient.getUserInfo(Long.valueOf(userId));

        if (userInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<WishListResponseDto> wishList = wishListService.getWishList(Long.valueOf(userId));
        return ResponseEntity.ok(wishList);
    }

    // 위시리스트 수정
    @PutMapping("/{id}")
    public ResponseEntity<WishListResponseDto> updateWishList(
            @PathVariable Long id,
            @Valid @RequestBody WishListUpdateRequestDto requestDto,
            @RequestHeader("X-User-Id") String userId) {

        // User service to validate user info
        UserInfoDto userInfo = userServiceClient.getUserInfo(Long.valueOf(userId));

        if (userInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WishListResponseDto updatedWishList = wishListService.updateWishList(id, requestDto, Long.valueOf(userId));
        return ResponseEntity.ok(updatedWishList);
    }

    // 위시리스트 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFromWishList(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {

        // User service to validate user info
        UserInfoDto userInfo = userServiceClient.getUserInfo(Long.valueOf(userId));

        if (userInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        wishListService.deleteFromWishList(id, Long.valueOf(userId));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

