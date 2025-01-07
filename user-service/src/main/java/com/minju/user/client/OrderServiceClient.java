//package com.minju.user.client;
//
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//import java.util.List;
//
//@FeignClient(name = "wishlist-service")
//public interface OrderServiceClient {
//    @GetMapping("/api/wishlist")
//    ResponseEntity<List<WishListResponseDto>> getWishList(@RequestParam("userId") Long userId);
//}
