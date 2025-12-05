package com.minju.common.dlq;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, Long> {

    List<DeadLetterEvent> findByStatus(String status);

    List<DeadLetterEvent> findByEventType(String eventType);

    @Query("SELECT d FROM DeadLetterEvent d WHERE d.status = 'PENDING' AND d.retryCount < :maxRetry ORDER BY d.createdAt ASC")
    List<DeadLetterEvent> findPendingEvents(int maxRetry);

    long countByStatus(String status);

    List<DeadLetterEvent> findByAggregateId(String aggregateId);
}
