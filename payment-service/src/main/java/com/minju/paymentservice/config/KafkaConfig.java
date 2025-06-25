package com.minju.paymentservice.config;

import com.minju.common.kafka.PaymentCompletedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaTemplate<String, PaymentCompletedEvent> paymentKafkaTemplate(ProducerFactory<String, PaymentCompletedEvent> pf) {
        return new KafkaTemplate<>(pf);
    }
}