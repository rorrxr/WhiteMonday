package com.minju.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PENDING' AND o.retryCount < :maxRetry ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingEvents(int maxRetry);

    List<OutboxEvent> findByStatusAndRetryCountLessThan(String status, int maxRetry);

    /**
     * 발행 완료된 이벤트 중 지정된 시간 이전의 이벤트 삭제
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.status = 'PUBLISHED' AND o.publishedAt < :before")
    int deletePublishedEventsBefore(@Param("before") LocalDateTime before);

    /**
     * 최종 실패한 이벤트 중 지정된 시간 이전의 이벤트 삭제
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.status = 'FAILED' AND o.createdAt < :before")
    int deleteFailedEventsBefore(@Param("before") LocalDateTime before);

    /**
     * 상태별 이벤트 개수 조회
     */
    long countByStatus(String status);
}