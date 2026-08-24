package com.example.demo01.quotetofile.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates filenames quote-<xx>.txt, with xx a 2-digit counter that cycles
 * 00-99 and wraps back to 00. The counter only lives in memory: a pod restart
 * resets it to 00 (files are picked up and moved out of input-01 within
 * seconds by file-to-kafka-app, so this is a harmless simplification).
 */
@Component
public class QuoteFilenameSequence {

    private final AtomicInteger counter = new AtomicInteger(0);

    public String next() {
        int number = counter.getAndUpdate(i -> (i + 1) % 100);
        return String.format("quote-%02d.txt", number);
    }
}
