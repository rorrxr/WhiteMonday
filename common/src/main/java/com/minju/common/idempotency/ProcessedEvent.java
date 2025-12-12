package com.minju.common.idempotency;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 처리된 이벤트 추적 엔티티 (멱등성 보장)
 * 동일 이벤트의 중복 처리를 방지합니다.
 */
@Entity
@Table(name = "processed_event", indexes = {
        @Index(name = "idx_processed_at", columnList = "processed_at"),
        @Index(name = "idx_aggregate_event", columnList = "aggregate_id,event_type")
})
@Getter
@Setter
@NoArgsConstructor
public class ProcessedEvent {

    /**
     * 이벤트 고유 ID (aggregateId_eventType_subId 형식)
     * 예: "123_STOCK_RESERVED_456" (orderId_eventType_productId)
     */
    @Id
    @Column(length = 255)
    private String eventId;

    /**
     * 집계 ID (주문 ID, 상품 ID 등)
     */
    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    /**
     * 이벤트 타입
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * 처리 시각
     */
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    /**
     * 처리한 서비스 이름
     */
    @Column(name = "processed_by", length = 50)
    private String processedBy;

    /**
     * 새 ProcessedEvent 생성
     */
    public static ProcessedEvent create(String aggregateId, String eventType, String subId, String processedBy) {
        ProcessedEvent event = new ProcessedEvent();
        event.setEventId(generateEventId(aggregateId, eventType, subId));
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setProcessedAt(LocalDateTime.now());
        event.setProcessedBy(processedBy);
        return event;
    }

    /**
     * 이벤트 ID 생성
     */
    public static String generateEventId(String aggregateId, String eventType, String subId) {
        if (subId != null && !subId.isEmpty()) {
            return aggregateId + "_" + eventType + "_" + subId;
        }
        return aggregateId + "_" + eventType;
    }
}
