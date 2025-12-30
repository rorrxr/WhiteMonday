package com.minju.order.repository;

import com.minju.order.dto.OrderSearchCondition;
import com.minju.order.entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepositoryCustom {

    /**
     * N+1 해결 - fetchJoin으로 OrderItems 함께 조회
     */
    List<Orders> findByUserIdWithItems(Long userId);

    /**
     * 동적 검색 - 상태/날짜/금액 조건 조합
     */
    Page<Orders> searchOrders(OrderSearchCondition condition, Pageable pageable);

    /**
     * 상태 업데이트 대상 조회 (findAll 대체)
     */
    List<Orders> findOrdersForStatusUpdate(List<String> statuses, LocalDateTime before);
}
