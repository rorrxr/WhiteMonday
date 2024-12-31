//package com.minju.whitemonday.order.repository;
//
//import com.minju.whitemonday.order.entity.Order;
//import com.minju.whitemonday.user.entity.User;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface OrderRepository extends JpaRepository<Order, Long> {
//    List<Order> findAllByUser(User user);
//
//    Optional<Order> findByOrderIdAndUser(Long orderId, User user); // 'id' 대신 'orderId' 사용
//}
