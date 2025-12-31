package com.minju.order.repository;

import com.minju.order.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Orders, Long>, OrderRepositoryCustom {
    List<Orders> findByUserId(Long userId);
}
