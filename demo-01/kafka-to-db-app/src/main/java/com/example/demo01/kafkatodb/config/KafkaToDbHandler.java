package com.example.demo01.kafkatodb.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Parses the incoming JSON message and inserts one row per message into the
 * "messages" table (filename, payload, creation_date). "payload" here is the
 * original file content carried in the JSON's "payload" field, not the full
 * JSON message.
 */
@Component
public class KafkaToDbHandler {

    private static final Logger log = LoggerFactory.getLogger(KafkaToDbHandler.class);
    private static final String INSERT_SQL =
            "INSERT INTO messages (filename, payload, creation_date) VALUES (?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public KafkaToDbHandler(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void process(String json) throws IOException {
        JsonNode node = objectMapper.readTree(json);
        String filename = node.path("filename").asText();
        String payload = node.path("payload").asText();
        if (filename.isBlank()) {
            throw new IOException("Kafka message did not contain a 'filename' field: " + json);
        }

        jdbcTemplate.update(INSERT_SQL, filename, payload, Timestamp.valueOf(LocalDateTime.now()));
        log.info("Inserted message for '{}' into 'messages' table", filename);
    }
}
