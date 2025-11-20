package org.example.cartservice.repository;

import org.example.cartservice.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.cartItems WHERE c.userId = :userId")
    Optional<Cart> findByUserIdWithItems(Long userId);

    Optional<Cart> findByUserId(Long userId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserId(Long userId);
}