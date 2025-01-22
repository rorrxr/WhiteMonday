package com.minju.wishlist.repository;

import com.minju.wishlist.entity.WishList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishListRepository extends JpaRepository<WishList, Long> {
    Optional<WishList> findByUserId(Long userId);

    Optional<WishList> findByUserIdAndProductId(Long userId, Long productId);

    Optional<WishList> findByIdAndUserId(Long id, Long userId);
}