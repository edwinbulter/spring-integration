package com.example.demo01.quotetofile.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Spring Integration inbound {@link MessageSource}: on every poll, checks
 * whether the configured period has elapsed (re-reading config.yml fresh each
 * time, so a change is picked up within ~1 second) and, if so, fetches a quote
 * and emits it as a message with the target filename as the FileHeaders.FILENAME
 * header. Returning null is the standard Spring Integration way of signalling
 * "no message this poll".
 */
@Component
public class QuoteMessageSource implements MessageSource<String> {

    private static final Logger log = LoggerFactory.getLogger(QuoteMessageSource.class);

    private final ConfigYamlReader configReader;
    private final ZenQuotesClient zenQuotesClient;
    private final QuoteFilenameSequence filenameSequence;

    // 0 so the very first poll immediately triggers a fetch on startup.
    private volatile long lastFetchEpochSeconds = 0;

    public QuoteMessageSource(ConfigYamlReader configReader,
                               ZenQuotesClient zenQuotesClient,
                               QuoteFilenameSequence filenameSequence) {
        this.configReader = configReader;
        this.zenQuotesClient = zenQuotesClient;
        this.filenameSequence = filenameSequence;
    }

    @Override
    public Message<String> receive() {
        long periodSeconds = configReader.getPeriodInSeconds();
        long now = Instant.now().getEpochSecond();
        if (now - lastFetchEpochSeconds < periodSeconds) {
            return null;
        }

        try {
            String quote = zenQuotesClient.fetchRandomQuote();
            String filename = filenameSequence.next();
            log.info("Fetched quote for '{}'", filename);
            return MessageBuilder.withPayload(quote)
                    .setHeader(FileHeaders.FILENAME, filename)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to fetch a quote: {}", e.getMessage());
            return null;
        } finally {
            // Always advance, even on failure, so a persistent outage doesn't
            // turn into a retry storm against the public ZenQuotes API.
            lastFetchEpochSeconds = now;
        }
    }
}
