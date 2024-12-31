//package com.minju.whitemonday.wishlist.service;
//
//import com.minju.whitemonday.wishlist.dto.WishListCreateRequestDto;
//import com.minju.whitemonday.wishlist.dto.WishListResponseDto;
//import com.minju.whitemonday.wishlist.dto.WishListUpdateRequestDto;
//import com.minju.whitemonday.product.entity.Product;
//import com.minju.whitemonday.user.entity.User;
//import com.minju.whitemonday.wishlist.entity.WishList;
//import com.minju.whitemonday.product.repository.ProductRepository;
//import com.minju.whitemonday.wishlist.repository.WishListRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class WishListService {
//
//    private final WishListRepository wishListRepository;
//    private final ProductRepository productRepository;
//
//    // 위시리스트에 상품 추가
//    @Transactional
//    public WishListResponseDto addToWishList(WishListCreateRequestDto requestDto, User user) {
//        Product product = productRepository.findById(requestDto.getProductId())
//                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));
//
//        WishList wishList = wishListRepository.findByUserAndProduct(user, product)
//                .orElse(new WishList(user, product, 0));
//
//        wishList.setQuantity(wishList.getQuantity() + requestDto.getQuantity());
//        WishList savedWishList = wishListRepository.save(wishList);
//        return new WishListResponseDto(savedWishList);
//    }
//
//    // 위시리스트 조회
//    @Transactional(readOnly = true)
//    public List<WishListResponseDto> getWishList(User user) {
//        List<WishList> wishLists = wishListRepository.findAllByUser(user);
//        return wishLists.stream()
//                .map(WishListResponseDto::new)
//                .collect(Collectors.toList());
//    }
//
//    // 위시리스트 항목 수정
//    @Transactional
//    public WishListResponseDto updateWishList(Long id, WishListUpdateRequestDto requestDto, User user) {
//        WishList wishList = wishListRepository.findByIdAndUser(id, user)
//                .orElseThrow(() -> new IllegalArgumentException("해당 위시리스트는 본인의 것이 아닙니다."));
//
//        wishList.updateQuantity(requestDto.getQuantity());
//        return new WishListResponseDto(wishList);
//    }
//
//    // 위시리스트 항목 삭제
//    @Transactional
//    public void deleteFromWishList(Long id, User user) {
//        WishList wishList = wishListRepository.findByIdAndUser(id, user)
//                .orElseThrow(() -> new IllegalArgumentException("해당 위시리스트는 본인의 것이 아닙니다."));
//
//        wishListRepository.delete(wishList);
//    }
//}