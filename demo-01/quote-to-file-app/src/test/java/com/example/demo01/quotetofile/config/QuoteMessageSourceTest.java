package com.example.demo01.quotetofile.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.file.FileHeaders;
import org.springframework.messaging.Message;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteMessageSourceTest {

    @Mock
    private ConfigYamlReader configReader;

    @Mock
    private ZenQuotesClient zenQuotesClient;

    @Mock
    private QuoteFilenameSequence filenameSequence;

    @Test
    void firstPollAlwaysFetchesAQuote() throws Exception {
        when(configReader.getPeriodInSeconds()).thenReturn(30L);
        when(zenQuotesClient.fetchRandomQuote()).thenReturn("Some quote");
        when(filenameSequence.next()).thenReturn("quote-00.txt");
        QuoteMessageSource source = new QuoteMessageSource(configReader, zenQuotesClient, filenameSequence);

        Message<String> message = source.receive();

        assertThat(message).isNotNull();
        assertThat(message.getPayload()).isEqualTo("Some quote");
        assertThat(message.getHeaders().get(FileHeaders.FILENAME)).isEqualTo("quote-00.txt");
    }

    @Test
    void pollWithinPeriodReturnsNullWithoutFetching() throws Exception {
        when(configReader.getPeriodInSeconds()).thenReturn(1000L);
        when(zenQuotesClient.fetchRandomQuote()).thenReturn("Some quote");
        when(filenameSequence.next()).thenReturn("quote-00.txt");
        QuoteMessageSource source = new QuoteMessageSource(configReader, zenQuotesClient, filenameSequence);

        source.receive();
        Message<String> secondPoll = source.receive();

        assertThat(secondPoll).isNull();
        verify(zenQuotesClient, org.mockito.Mockito.times(1)).fetchRandomQuote();
    }

    @Test
    void returnsNullWithoutPropagatingWhenFetchFails() throws Exception {
        when(configReader.getPeriodInSeconds()).thenReturn(30L);
        when(zenQuotesClient.fetchRandomQuote()).thenThrow(new IOException("boom"));
        QuoteMessageSource source = new QuoteMessageSource(configReader, zenQuotesClient, filenameSequence);

        Message<String> message = source.receive();

        assertThat(message).isNull();
        verify(filenameSequence, never()).next();
    }
}
