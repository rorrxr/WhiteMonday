package com.minju.whitemonday.order.repository;

import com.minju.whitemonday.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
