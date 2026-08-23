package com.example.demo01.quotetofile.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Writes each quote to input-dir/quote-<xx>.txt, with xx a 2-digit counter that
 * cycles 00-99 and wraps back to 00. The counter only lives in memory: a pod
 * restart resets it to 00 (files are picked up and moved out of input-01 within
 * seconds by file-to-kafka-app, so this is a harmless simplification).
 */
@Component
public class QuoteFileWriter {

    private static final Logger log = LoggerFactory.getLogger(QuoteFileWriter.class);

    private final Path inputDir;
    private final AtomicInteger counter = new AtomicInteger(0);

    public QuoteFileWriter(@Value("${quote.input-dir}") String inputDir) {
        this.inputDir = Path.of(inputDir);
    }

    public void writeQuote(String quote) throws IOException {
        int number = counter.getAndUpdate(i -> (i + 1) % 100);
        String filename = String.format("quote-%02d.txt", number);

        Files.createDirectories(inputDir);
        Path target = inputDir.resolve(filename);
        Files.writeString(target, quote, StandardCharsets.UTF_8);
        log.info("Wrote quote to '{}'", target);
    }
}
