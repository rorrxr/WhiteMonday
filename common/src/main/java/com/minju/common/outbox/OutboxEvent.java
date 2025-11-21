package com.minju.common.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event", indexes = {
        @Index(name = "idx_status_retry", columnList = "status,retry_count"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter @Setter
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String aggregateType; // ORDER, PAYMENT, STOCK

    @Column(nullable = false, length = 100)
    private String aggregateId;

    @Column(nullable = false, length = 100)
    private String eventType; // STOCK_RESERVATION_REQUESTED, PAYMENT_REQUESTED 등

    @Column(nullable = false, length = 50)
    private String topic; // Kafka 토픽 이름

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload; // JSON 형태의 이벤트 데이터

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, PUBLISHED, FAILED

    @Column(nullable = false)
    private Integer retryCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }
}