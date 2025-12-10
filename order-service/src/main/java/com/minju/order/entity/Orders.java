package com.minju.order.entity;

import jakarta.persistence.Entity;
import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false, columnDefinition = "BIGINT")
    private Long id; // Primary Key

    @Column(name = "user_id", nullable = false, columnDefinition = "BIGINT")
    private Long userId;

    @Column(name = "order_status", nullable = false)
    private String orderStatus;

    @Column(name = "total_amount", nullable = false)
    private int totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int reserved = 0; // 기본값 0

    /**
     * 총 상품 종류 수 (다중 상품 주문 Saga 동기화용)
     */
    @Column(nullable = false, columnDefinition = "int default 0")
    private int totalItemCount = 0;

    /**
     * 재고 예약 완료된 상품 종류 수
     */
    @Column(nullable = false, columnDefinition = "int default 0")
    private int reservedItemCount = 0;

    /**
     * 재고 예약 실패한 상품 종류 수
     */
    @Column(nullable = false, columnDefinition = "int default 0")
    private int failedItemCount = 0;

    /**
     * 모든 상품의 재고 예약이 완료되었는지 확인
     */
    public boolean isAllItemsReserved() {
        return totalItemCount > 0 && reservedItemCount == totalItemCount;
    }

    /**
     * 재고 예약이 하나라도 실패했는지 확인
     */
    public boolean hasAnyFailedReservation() {
        return failedItemCount > 0;
    }

    /**
     * 모든 재고 예약 처리가 완료되었는지 확인 (성공+실패)
     */
    public boolean isAllReservationsProcessed() {
        return totalItemCount > 0 && (reservedItemCount + failedItemCount) == totalItemCount;
    }
}