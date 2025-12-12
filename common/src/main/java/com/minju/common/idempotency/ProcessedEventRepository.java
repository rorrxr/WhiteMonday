package com.minju.common.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

    /**
     * 이벤트 ID로 존재 여부 확인
     */
    boolean existsByEventId(String eventId);

    /**
     * 집계 ID와 이벤트 타입으로 조회
     */
    List<ProcessedEvent> findByAggregateIdAndEventType(String aggregateId, String eventType);

    /**
     * 특정 시간 이전의 처리된 이벤트 삭제 (정리용)
     * 기본: 7일 이전 이벤트 삭제
     */
    @Modifying
    @Query("DELETE FROM ProcessedEvent p WHERE p.processedAt < :before")
    int deleteProcessedEventsBefore(@Param("before") LocalDateTime before);

    /**
     * 서비스별 처리된 이벤트 개수
     */
    long countByProcessedBy(String processedBy);
}
