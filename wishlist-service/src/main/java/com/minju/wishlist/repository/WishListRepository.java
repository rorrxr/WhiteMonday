package com.minju.wishlist.repository;

import com.minju.wishlist.entity.WishList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishListRepository extends JpaRepository<WishList, Long> {
    List<WishList> findAllByUser(User user);

    Optional<WishList> findByUserAndProduct(User user, Product product);

    Optional<WishList> findByIdAndUser(Long id, User user);
}
