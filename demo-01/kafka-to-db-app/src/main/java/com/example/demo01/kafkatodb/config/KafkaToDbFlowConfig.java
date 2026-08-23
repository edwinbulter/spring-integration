package com.example.demo01.kafkatodb.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.messaging.MessagingException;

import java.io.IOException;

@Configuration
public class KafkaToDbFlowConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
    }

    @Bean
    public IntegrationFlow kafkaToDbFlow(ConsumerFactory<String, String> consumerFactory,
                                          KafkaToDbHandler handler,
                                          @Value("${kafka.topic}") String topic) {
        return IntegrationFlow
                .from(Kafka.messageDrivenChannelAdapter(consumerFactory, topic))
                .<String>handle((json, headers) -> {
                    try {
                        handler.process(json);
                    } catch (IOException e) {
                        throw new MessagingException("Failed to persist message to database", e);
                    }
                    return null;
                })
                .get();
    }
}
