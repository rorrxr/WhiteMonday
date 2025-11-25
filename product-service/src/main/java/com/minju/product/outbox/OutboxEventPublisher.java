package com.minju.product.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minju.common.outbox.OutboxEvent;
import com.minju.common.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Outbox에 이벤트 저장 (트랜잭션 내에서 호출 필수)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveEvent(String aggregateType, String aggregateId,
                          String eventType, String topic, Object eventData) {
        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateType(aggregateType);
            outboxEvent.setAggregateId(aggregateId);
            outboxEvent.setEventType(eventType);
            outboxEvent.setTopic(topic);
            outboxEvent.setPayload(objectMapper.writeValueAsString(eventData));

            outboxRepository.save(outboxEvent);
            log.info("[Product] Outbox 이벤트 저장 성공 - type: {}, id: {}", eventType, aggregateId);

        } catch (Exception e) {
            log.error("[Product] Outbox 이벤트 저장 실패: ", e);
            throw new RuntimeException("이벤트 저장 실패", e);
        }
    }

    /**
     * Outbox 이벤트를 Kafka로 발행 (스케줄러)
     */
    @Scheduled(fixedDelay = 5000) // 5초마다 실행
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents(3);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("[Product] Outbox 이벤트 발행 시작 - 대기 중인 이벤트: {}개", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // Kafka로 발행
                Object eventData = deserializeEvent(event);
                kafkaTemplate.send(event.getTopic(), eventData)
                        .get(5, TimeUnit.SECONDS);

                // 발행 성공
                event.setStatus("PUBLISHED");
                event.setPublishedAt(LocalDateTime.now());
                outboxRepository.save(event);

                log.info("[Product] Outbox 이벤트 발행 성공 - type: {}, topic: {}",
                        event.getEventType(), event.getTopic());

            } catch (Exception e) {
                log.error("[Product] Outbox 이벤트 발행 실패 - type: {}, retry: {}",
                        event.getEventType(), event.getRetryCount(), e);

                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 3) {
                    event.setStatus("FAILED");
                    log.error("[Product] Outbox 이벤트 최종 실패 - id: {}", event.getId());
                }
                outboxRepository.save(event);
            }
        }
    }

    private Object deserializeEvent(OutboxEvent outboxEvent) throws Exception {
        Class<?> eventClass = getEventClass(outboxEvent.getEventType());
        return objectMapper.readValue(outboxEvent.getPayload(), eventClass);
    }

    private Class<?> getEventClass(String eventType) {
        return switch (eventType) {
            case "STOCK_RESERVED" ->
                    com.minju.common.kafka.StockReservedEvent.class;
            case "STOCK_RESERVATION_FAILED" ->
                    com.minju.common.kafka.StockReservationFailedEvent.class;
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}