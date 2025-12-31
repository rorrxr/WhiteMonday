package com.minju.order.repository;

import com.minju.order.dto.OrderSearchCondition;
import com.minju.order.entity.Orders;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.minju.order.entity.QOrderItem.orderItem;
import static com.minju.order.entity.QOrders.orders;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * N+1 해결 - fetchJoin으로 OrderItems 함께 조회
     */
    @Override
    public List<Orders> findByUserIdWithItems(Long userId) {
        return queryFactory
                .selectFrom(orders)
                .leftJoin(orders.orderItems, orderItem).fetchJoin()
                .where(orders.userId.eq(userId))
                .orderBy(orders.createdAt.desc())
                .fetch();
    }

    /**
     * 동적 검색 - 상태/날짜/금액 조건 조합
     */
    @Override
    public Page<Orders> searchOrders(OrderSearchCondition condition, Pageable pageable) {
        // 카운트 쿼리
        Long total = queryFactory
                .select(orders.count())
                .from(orders)
                .where(
                        userIdEq(condition.getUserId()),
                        orderStatusEq(condition.getOrderStatus()),
                        createdAtGoe(condition.getStartDate()),
                        createdAtLoe(condition.getEndDate()),
                        totalAmountGoe(condition.getMinAmount()),
                        totalAmountLoe(condition.getMaxAmount())
                )
                .fetchOne();

        // 데이터 쿼리 (fetchJoin으로 N+1 방지)
        List<Orders> content = queryFactory
                .selectFrom(orders)
                .leftJoin(orders.orderItems, orderItem).fetchJoin()
                .where(
                        userIdEq(condition.getUserId()),
                        orderStatusEq(condition.getOrderStatus()),
                        createdAtGoe(condition.getStartDate()),
                        createdAtLoe(condition.getEndDate()),
                        totalAmountGoe(condition.getMinAmount()),
                        totalAmountLoe(condition.getMaxAmount())
                )
                .orderBy(orders.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 상태 업데이트 대상 조회 (findAll 대체)
     */
    @Override
    public List<Orders> findOrdersForStatusUpdate(List<String> statuses, LocalDateTime before) {
        return queryFactory
                .selectFrom(orders)
                .where(
                        orders.orderStatus.in(statuses),
                        orders.createdAt.before(before)
                )
                .fetch();
    }

    // ==================== 조건 빌더 메서드 ====================

    private BooleanExpression userIdEq(Long userId) {
        return userId != null ? orders.userId.eq(userId) : null;
    }

    private BooleanExpression orderStatusEq(String orderStatus) {
        return orderStatus != null ? orders.orderStatus.eq(orderStatus) : null;
    }

    private BooleanExpression createdAtGoe(LocalDateTime startDate) {
        return startDate != null ? orders.createdAt.goe(startDate) : null;
    }

    private BooleanExpression createdAtLoe(LocalDateTime endDate) {
        return endDate != null ? orders.createdAt.loe(endDate) : null;
    }

    private BooleanExpression totalAmountGoe(Integer minAmount) {
        return minAmount != null ? orders.totalAmount.goe(minAmount) : null;
    }

    private BooleanExpression totalAmountLoe(Integer maxAmount) {
        return maxAmount != null ? orders.totalAmount.loe(maxAmount) : null;
    }
}
