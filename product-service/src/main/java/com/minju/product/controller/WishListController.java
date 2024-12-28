package com.minju.product.controller;

import com.minju.product.dto.WishListCreateRequestDto;
import com.minju.product.dto.WishListResponseDto;
import com.minju.product.service.WishListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
public class WishListController {

    private final WishListService wishListService;

    // Add item to wishlist
    @PostMapping
    public ResponseEntity<WishListResponseDto> addToWishList(
            @Valid @RequestBody WishListCreateRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WishListResponseDto savedWishList = wishListService.addToWishList(requestDto, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedWishList);
    }

    // Get wishlist items
    @GetMapping
    public ResponseEntity<List<WishListResponseDto>> getWishList(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<WishListResponseDto> wishList = wishListService.getWishList(userDetails.getUser());
        return ResponseEntity.ok(wishList);
    }

    // Update wishlist item
    @PutMapping("/{id}")
    public ResponseEntity<WishListResponseDto> updateWishList(
            @PathVariable Long id,
            @Valid @RequestBody WishListUpdateRequestDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WishListResponseDto updatedWishList = wishListService.updateWishList(id, requestDto, userDetails.getUser());
        return ResponseEntity.ok(updatedWishList);
    }

    // Delete wishlist item
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFromWishList(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        wishListService.deleteFromWishList(id, userDetails.getUser());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
