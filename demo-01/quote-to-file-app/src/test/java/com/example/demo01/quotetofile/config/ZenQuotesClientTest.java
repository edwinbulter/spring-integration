package com.example.demo01.quotetofile.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZenQuotesClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsQFieldOnHappyPath() throws IOException, InterruptedException {
        stubResponse(200, "[{\"q\":\"Some quote\",\"a\":\"Some author\"}]");
        ZenQuotesClient client = new ZenQuotesClient(objectMapper, httpClient);

        assertThat(client.fetchRandomQuote()).isEqualTo("Some quote");
    }

    @Test
    void throwsWhenStatusIsNotOk() throws IOException, InterruptedException {
        stubResponse(500, "Internal Server Error");
        ZenQuotesClient client = new ZenQuotesClient(objectMapper, httpClient);

        assertThatThrownBy(client::fetchRandomQuote)
                .isInstanceOf(IOException.class)
                .hasMessageContaining("HTTP 500");
    }

    @Test
    void throwsWhenResponseArrayIsEmpty() throws IOException, InterruptedException {
        stubResponse(200, "[]");
        ZenQuotesClient client = new ZenQuotesClient(objectMapper, httpClient);

        assertThatThrownBy(client::fetchRandomQuote)
                .isInstanceOf(IOException.class);
    }

    @Test
    void throwsWhenQFieldIsMissing() throws IOException, InterruptedException {
        stubResponse(200, "[{\"a\":\"Some author\"}]");
        ZenQuotesClient client = new ZenQuotesClient(objectMapper, httpClient);

        assertThatThrownBy(client::fetchRandomQuote)
                .isInstanceOf(IOException.class)
                .hasMessageContaining("'q' field");
    }

    @SuppressWarnings("unchecked")
    private void stubResponse(int statusCode, String body) throws IOException, InterruptedException {
        when(httpResponse.statusCode()).thenReturn(statusCode);
        when(httpResponse.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
    }
}
