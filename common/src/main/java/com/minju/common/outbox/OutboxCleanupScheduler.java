package com.minju.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxCleanupScheduler {

    private final OutboxEventRepository outboxRepository;

    @Value("${outbox.cleanup.published-retention-hours:24}")
    private int publishedRetentionHours;

    @Value("${outbox.cleanup.failed-retention-days:7}")
    private int failedRetentionDays;

    /**
     * 발행 완료된 Outbox 이벤트 정리 (1시간마다 실행)
     * 기본: 24시간 이전의 PUBLISHED 이벤트 삭제
     */
    @Scheduled(fixedDelayString = "${outbox.cleanup.interval-ms:3600000}")
    @Transactional
    public void cleanupPublishedEvents() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusHours(publishedRetentionHours);

            int deletedCount = outboxRepository.deletePublishedEventsBefore(threshold);

            if (deletedCount > 0) {
                log.info("[Outbox Cleanup] PUBLISHED 이벤트 {}건 삭제 (기준: {}시간 이전)",
                        deletedCount, publishedRetentionHours);
            }

        } catch (Exception e) {
            log.error("[Outbox Cleanup] PUBLISHED 이벤트 정리 실패: ", e);
        }
    }

    /**
     * 실패한 Outbox 이벤트 정리 (매일 새벽 3시 실행)
     * 기본: 7일 이전의 FAILED 이벤트 삭제
     */
    @Scheduled(cron = "${outbox.cleanup.failed-cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupFailedEvents() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(failedRetentionDays);

            int deletedCount = outboxRepository.deleteFailedEventsBefore(threshold);

            if (deletedCount > 0) {
                log.info("[Outbox Cleanup] FAILED 이벤트 {}건 삭제 (기준: {}일 이전)",
                        deletedCount, failedRetentionDays);
            }

        } catch (Exception e) {
            log.error("[Outbox Cleanup] FAILED 이벤트 정리 실패: ", e);
        }
    }

    /**
     * Outbox 상태 로깅 (10분마다)
     */
    @Scheduled(fixedDelayString = "${outbox.cleanup.status-log-interval-ms:600000}")
    public void logOutboxStatus() {
        try {
            long pendingCount = outboxRepository.countByStatus("PENDING");
            long publishedCount = outboxRepository.countByStatus("PUBLISHED");
            long failedCount = outboxRepository.countByStatus("FAILED");

            if (pendingCount > 0 || failedCount > 0) {
                log.info("[Outbox Status] PENDING: {}, PUBLISHED: {}, FAILED: {}",
                        pendingCount, publishedCount, failedCount);
            }

            // PENDING이 많이 쌓이면 경고
            if (pendingCount > 100) {
                log.warn("[Outbox Alert] PENDING 이벤트가 {}건 누적됨 - 발행 지연 확인 필요", pendingCount);
            }

            // FAILED가 있으면 경고
            if (failedCount > 0) {
                log.warn("[Outbox Alert] FAILED 이벤트가 {}건 존재 - 수동 확인 필요", failedCount);
            }

        } catch (Exception e) {
            log.error("[Outbox Status] 상태 조회 실패: ", e);
        }
    }
}
