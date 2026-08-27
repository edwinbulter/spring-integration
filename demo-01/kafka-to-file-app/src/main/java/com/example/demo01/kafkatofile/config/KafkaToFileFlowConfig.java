package com.example.demo01.kafkatofile.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;

import java.io.IOException;

@Configuration
public class KafkaToFileFlowConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaToFileFlowConfig.class);
    private static final String ERROR_CHANNEL_NAME = "kafkaToFileErrorChannel";

    @Bean
    public ConsumerFactory<String, String> consumerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
    }

    @Bean
    public IntegrationFlow kafkaToFileFlow(ConsumerFactory<String, String> consumerFactory,
                                            KafkaToFileHandler handler,
                                            @Value("${kafka.topic}") String topic) {
        return IntegrationFlow
                // messageDrivenChannelAdapter ontvangt messages van het topic via push, mislukte berichten gaan naar het error-channel:
                .from(Kafka.messageDrivenChannelAdapter(consumerFactory, topic).errorChannel(ERROR_CHANNEL_NAME))
                // laat handler de message verwerken:
                .<String>handle((json, headers) -> {
                    try {
                        handler.process(json);
                    } catch (IOException e) {
                        throw new MessagingException("Failed to write file for message", e);
                    }
                    return null;
                })
                .get();
    }

    @Bean(ERROR_CHANNEL_NAME)
    public MessageChannel kafkaToFileErrorChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow kafkaToFileErrorLoggingFlow() {
        return IntegrationFlow.from(ERROR_CHANNEL_NAME)
                .handle((GenericHandler<ErrorMessage>) (errorMessage, headers) -> {
                    Throwable cause = errorMessage.getPayload();
                    Message<?> failedMessage = cause instanceof MessagingException messagingException
                            ? messagingException.getFailedMessage()
                            : null;
                    log.error("kafka-to-file-app failed to process message: payload={}, headers={}",
                            failedMessage != null ? failedMessage.getPayload() : null,
                            failedMessage != null ? failedMessage.getHeaders() : null,
                            cause);
                    return null;
                })
                .get();
    }
}
