package com.example.demo01.quotetofile.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteFilenameSequenceTest {

    @Test
    void startsAtZeroAndZeroPadsSingleDigits() {
        QuoteFilenameSequence sequence = new QuoteFilenameSequence();

        assertThat(sequence.next()).isEqualTo("quote-00.txt");
        assertThat(sequence.next()).isEqualTo("quote-01.txt");
    }

    @Test
    void wrapsAroundAfterNinetyNine() {
        QuoteFilenameSequence sequence = new QuoteFilenameSequence();
        for (int i = 0; i < 100; i++) {
            sequence.next();
        }

        assertThat(sequence.next()).isEqualTo("quote-00.txt");
    }
}
