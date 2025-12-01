package com.minju.wishlist.controller;

import com.minju.common.dto.CommonResponse;
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
    public ResponseEntity<CommonResponse<WishListResponseDto>> addWishList(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid WishListCreateRequestDto requestDto) {

        WishListResponseDto response = wishListService.addWishList(requestDto, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(
                        HttpStatus.CREATED.value(),
                        "위시리스트가 추가되었습니다.",
                        response
                ));
    }

    // 위시리스트 조회 (GET /api/wishlist)
    @GetMapping
    public ResponseEntity<CommonResponse<List<WishListResponseDto>>> getWishLists(
            @RequestHeader("X-User-Id") Long userId) {

        List<WishListResponseDto> wishLists = wishListService.getWishLists(userId);

        return ResponseEntity.ok(
                CommonResponse.success("위시리스트 조회에 성공했습니다.", wishLists)
        );
    }

    // 위시리스트 수정 (PUT /api/wishlist/{id})
    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<WishListResponseDto>> updateWishList(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid WishListUpdateRequestDto requestDto) {

        WishListResponseDto response = wishListService.updateWishList(id, requestDto, userId);

        return ResponseEntity.ok(
                CommonResponse.success("위시리스트가 수정되었습니다.", response)
        );
    }

    // 위시리스트 삭제 (DELETE /api/wishlist/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> deleteWishList(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        wishListService.deleteWishListItem(id, userId);

        // 204를 쓰면 body가 없어야 해서, 공통 응답을 유지하려면 200 + 메시지가 더 일관적임
        return ResponseEntity.ok(
                CommonResponse.success("위시리스트가 삭제되었습니다.", null)
        );
    }
}
