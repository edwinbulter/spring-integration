package com.example.demo01.quotetofile.config;

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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;

import java.io.File;

@Configuration
public class QuoteToFileFlowConfig {

    private static final Logger log = LoggerFactory.getLogger(QuoteToFileFlowConfig.class);
    private static final String ERROR_CHANNEL_NAME = "quoteToFileErrorChannel";

    @Bean
    public IntegrationFlow quoteToFileFlow(QuoteMessageSource quoteMessageSource,
                                            @Value("${quote.input-dir}") String inputDir,
                                            @Value("${quote.poll-interval-ms}") long pollIntervalMs) {
        return IntegrationFlow
                // roep quoteMessageSource.receive elke pollIntervalMs ms aan, mislukte berichten gaan naar het error-channel:
                .from(quoteMessageSource, e -> e.poller(Pollers.fixedDelay(pollIntervalMs).errorChannel(ERROR_CHANNEL_NAME)))
                // gebruik de Message die QuoteMessageSource.receive aanmaakt en saved die in inputDir:
                .handle(Files.outboundAdapter(new File(inputDir)).autoCreateDirectory(true))
                .get();
    }

    @Bean(ERROR_CHANNEL_NAME)
    public MessageChannel quoteToFileErrorChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow quoteToFileErrorLoggingFlow() {
        return IntegrationFlow.from(ERROR_CHANNEL_NAME)
                .handle((GenericHandler<ErrorMessage>) (errorMessage, headers) -> {
                    Throwable cause = errorMessage.getPayload();
                    Message<?> failedMessage = cause instanceof MessagingException messagingException
                            ? messagingException.getFailedMessage()
                            : null;
                    log.error("quote-to-file-app failed to process message: payload={}, headers={}",
                            failedMessage != null ? failedMessage.getPayload() : null,
                            failedMessage != null ? failedMessage.getHeaders() : null,
                            cause);
                    return null;
                })
                .get();
    }
}
