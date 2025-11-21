package com.minju.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PENDING' AND o.retryCount < :maxRetry ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingEvents(int maxRetry);

    List<OutboxEvent> findByStatusAndRetryCountLessThan(String status, int maxRetry);
}