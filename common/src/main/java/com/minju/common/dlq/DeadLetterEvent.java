package com.minju.common.dlq;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dead_letter_event", indexes = {
        @Index(name = "idx_dlq_status", columnList = "status"),
        @Index(name = "idx_dlq_event_type", columnList = "event_type"),
        @Index(name = "idx_dlq_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class DeadLetterEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 50)
    private String originalTopic;

    @Column(nullable = false, length = 100)
    private String aggregateId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(length = 50)
    private String errorType;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(nullable = false)
    private Integer retryCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    @Column(length = 500)
    private String processedBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static DeadLetterEvent create(String eventType, String originalTopic,
                                          String aggregateId, String payload,
                                          String errorMessage, String errorType) {
        DeadLetterEvent event = new DeadLetterEvent();
        event.setEventType(eventType);
        event.setOriginalTopic(originalTopic);
        event.setAggregateId(aggregateId);
        event.setPayload(payload);
        event.setErrorMessage(errorMessage);
        event.setErrorType(errorType);
        event.setStatus("PENDING");
        event.setRetryCount(0);
        return event;
    }

    public void markAsProcessed(String processedBy) {
        this.status = "PROCESSED";
        this.processedAt = LocalDateTime.now();
        this.processedBy = processedBy;
    }

    public void markAsFailed() {
        this.status = "FAILED";
        this.retryCount++;
    }
}
