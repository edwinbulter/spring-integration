package com.example.demo01.quotetofile.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Re-reads the external config.yml on every call (no caching), so that a change
 * to QuoteToFile.periodInSeconds is picked up on the very next 1-second tick.
 * Falls back to the configured default whenever the file is missing, unreadable
 * or does not contain a valid value. Logs whenever the effective period changes.
 */
@Component
public class ConfigYamlReader {

    private static final Logger log = LoggerFactory.getLogger(ConfigYamlReader.class);
    private static final long UNSET = Long.MIN_VALUE;

    private final Path configFile;
    private final long defaultPeriodSeconds;
    private final Yaml yaml = new Yaml();
    // Avoids logging the same warning on every single tick.
    private final AtomicReference<String> lastWarning = new AtomicReference<>();
    // Tracks the last effective period so changes can be logged.
    private final AtomicLong lastPeriodSeconds = new AtomicLong(UNSET);

    public ConfigYamlReader(@Value("${quote.config-file}") String configFile,
                             @Value("${quote.default-period-seconds}") long defaultPeriodSeconds) {
        this.configFile = Path.of(configFile);
        this.defaultPeriodSeconds = defaultPeriodSeconds;
    }

    public long getPeriodInSeconds() {
        long periodSeconds = resolvePeriodInSeconds();
        logIfChanged(periodSeconds);
        return periodSeconds;
    }

    private long resolvePeriodInSeconds() {
        try (InputStream in = Files.newInputStream(configFile)) {
            Object loaded = yaml.load(in);
            if (loaded instanceof Map<?, ?> root) {
                Object section = root.get("QuoteToFile");
                if (section instanceof Map<?, ?> quoteToFile) {
                    Object period = quoteToFile.get("periodInSeconds");
                    if (period instanceof Number number) {
                        warnOnce(null);
                        return number.longValue();
                    }
                }
            }
            warnOnce("'" + configFile + "' does not contain a numeric QuoteToFile.periodInSeconds");
        } catch (IOException e) {
            warnOnce("Could not read '" + configFile + "': " + e.getMessage());
        } catch (RuntimeException e) {
            warnOnce("Could not parse '" + configFile + "': " + e.getMessage());
        }
        return defaultPeriodSeconds;
    }

    private void logIfChanged(long periodSeconds) {
        long previous = lastPeriodSeconds.getAndSet(periodSeconds);
        if (previous == UNSET) {
            log.info("QuoteToFile.periodInSeconds is {}s", periodSeconds);
        } else if (previous != periodSeconds) {
            log.info("QuoteToFile.periodInSeconds changed from {}s to {}s", previous, periodSeconds);
        }
    }

    private void warnOnce(String message) {
        String previous = lastWarning.getAndSet(message);
        if (message != null && !message.equals(previous)) {
            log.warn("{} - falling back to default period of {}s", message, defaultPeriodSeconds);
        }
    }
}
