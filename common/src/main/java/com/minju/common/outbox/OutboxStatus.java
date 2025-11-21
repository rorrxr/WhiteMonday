package com.minju.common.outbox;

public enum OutboxStatus {
    PENDING,   // 아직 Kafka로 안 나간 상태
    SENT,      // 정상 발행 완료
    FAILED     // 발행 실패, 재시도 대상
}