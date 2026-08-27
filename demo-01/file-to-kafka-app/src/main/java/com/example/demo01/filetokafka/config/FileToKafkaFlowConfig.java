package com.example.demo01.filetokafka.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;

import java.io.File;
import java.io.IOException;

@Configuration
public class FileToKafkaFlowConfig {

    private static final Logger log = LoggerFactory.getLogger(FileToKafkaFlowConfig.class);
    private static final String ERROR_CHANNEL_NAME = "fileToKafkaErrorChannel";

    @Bean
    public IntegrationFlow fileToKafkaFlow(FileToKafkaHandler handler,
                                            @Value("${file.input-dir}") String inputDir,
                                            @Value("${file.poll-delay-ms}") long pollDelayMs) {
        return IntegrationFlow
                .from(Files.inboundAdapter(new File(inputDir))
                                .autoCreateDirectory(true)
                                // vervang de default filter omdat we bestanden met dezelfde filenaam toestaan.
                                .filter(new IgnoreHiddenFileListFilter()),
                        // poll voor nieuwe bestanden met pollDelayMs, mislukte berichten gaan naar het error-channel
                        e -> e.poller(Pollers.fixedDelay(pollDelayMs).errorChannel(ERROR_CHANNEL_NAME)))
                // verwerk ontvangen files met de handler
                .<File>handle((file, headers) -> {
                    try {
                        handler.process(file);
                    } catch (IOException e) {
                        throw new MessagingException("Failed to process file " + file, e);
                    }
                    return null;
                })
                .get();
    }

    @Bean(ERROR_CHANNEL_NAME)
    public MessageChannel fileToKafkaErrorChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow fileToKafkaErrorLoggingFlow() {
        return IntegrationFlow.from(ERROR_CHANNEL_NAME)
                .handle((GenericHandler<ErrorMessage>) (errorMessage, headers) -> {
                    Throwable cause = errorMessage.getPayload();
                    Message<?> failedMessage = cause instanceof MessagingException messagingException
                            ? messagingException.getFailedMessage()
                            : null;
                    log.error("file-to-kafka-app failed to process message: payload={}, headers={}",
                            failedMessage != null ? failedMessage.getPayload() : null,
                            failedMessage != null ? failedMessage.getHeaders() : null,
                            cause);
                    return null;
                })
                .get();
    }
}
