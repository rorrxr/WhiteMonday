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
}