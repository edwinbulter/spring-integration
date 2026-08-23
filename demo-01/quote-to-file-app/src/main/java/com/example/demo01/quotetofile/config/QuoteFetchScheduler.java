package com.example.demo01.quotetofile.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Ticks every second and re-reads the configured period on every tick, so a
 * change to QuoteToFile.periodInSeconds in config.yml is picked up as fast as
 * possible (within ~1 second) without needing a restart or a filesystem
 * watcher.
 */
@Component
public class QuoteFetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(QuoteFetchScheduler.class);

    private final ConfigYamlReader configReader;
    private final ZenQuotesClient zenQuotesClient;
    private final QuoteFileWriter quoteFileWriter;

    // 0 so the very first tick immediately triggers a fetch on startup.
    private volatile long lastFetchEpochSeconds = 0;

    public QuoteFetchScheduler(ConfigYamlReader configReader,
                                ZenQuotesClient zenQuotesClient,
                                QuoteFileWriter quoteFileWriter) {
        this.configReader = configReader;
        this.zenQuotesClient = zenQuotesClient;
        this.quoteFileWriter = quoteFileWriter;
    }

    @Scheduled(fixedDelay = 1000)
    public void tick() {
        long periodSeconds = configReader.getPeriodInSeconds();
        long now = Instant.now().getEpochSecond();
        if (now - lastFetchEpochSeconds < periodSeconds) {
            return;
        }

        try {
            String quote = zenQuotesClient.fetchRandomQuote();
            quoteFileWriter.writeQuote(quote);
        } catch (Exception e) {
            log.warn("Failed to fetch/write a quote: {}", e.getMessage());
        } finally {
            // Always advance, even on failure, so a persistent outage doesn't
            // turn into a retry storm against the public ZenQuotes API.
            lastFetchEpochSeconds = now;
        }
    }
}
