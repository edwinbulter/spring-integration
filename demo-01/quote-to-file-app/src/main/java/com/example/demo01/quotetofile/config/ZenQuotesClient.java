package com.example.demo01.quotetofile.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches a single random quote from the public ZenQuotes API using the JDK's
 * built-in HTTP client, so no extra HTTP dependency is needed.
 */
@Component
public class ZenQuotesClient {

    private static final URI RANDOM_QUOTE_URI = URI.create("https://zenquotes.io/api/random");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ZenQuotesClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    // Package-private: allows tests to inject a mock HttpClient instead of hitting the real API.
    ZenQuotesClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * @return the "q" field of the random quote returned by ZenQuotes.
     */
    public String fetchRandomQuote() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(RANDOM_QUOTE_URI)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("ZenQuotes returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode array = objectMapper.readTree(response.body());
        if (!array.isArray() || array.isEmpty()) {
            throw new IOException("Unexpected ZenQuotes response: " + response.body());
        }
        String quote = array.get(0).path("q").asText(null);
        if (quote == null || quote.isBlank()) {
            throw new IOException("ZenQuotes response did not contain a 'q' field: " + response.body());
        }
        return quote;
    }
}
