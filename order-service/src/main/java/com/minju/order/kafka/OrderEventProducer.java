package com.minju.order.kafka;

import com.minju.common.kafka.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(OrderCreatedEvent event) {
        kafkaTemplate.send("order-created-topic", event);
        log.info("Kafka - 주문 생성 이벤트 발행: {}", event);
    }
}
