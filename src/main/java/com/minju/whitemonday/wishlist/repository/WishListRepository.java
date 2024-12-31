//package com.minju.whitemonday.wishlist.repository;
//
//import com.minju.whitemonday.product.entity.Product;
//import com.minju.whitemonday.user.entity.User;
//import com.minju.whitemonday.wishlist.entity.WishList;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface WishListRepository extends JpaRepository<WishList, Long> {
//    List<WishList> findAllByUser(User user);
//
//    Optional<WishList> findByUserAndProduct(User user, Product product);
//
//    Optional<WishList> findByIdAndUser(Long id, User user);
//}
