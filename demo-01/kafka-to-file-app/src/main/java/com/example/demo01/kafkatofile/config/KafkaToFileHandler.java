package com.example.demo01.kafkatofile.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Parses the incoming JSON message to determine the target filename, appends a
 * logging entry recording receipt from topic-01, and writes the resulting JSON
 * to the output folder under that filename.
 */
@Component
public class KafkaToFileHandler {

    private static final Logger log = LoggerFactory.getLogger(KafkaToFileHandler.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String RECEIVED_LOG_MESSAGE = "message received from topic-01";

    private final ObjectMapper objectMapper;
    private final Path outputDir;

    public KafkaToFileHandler(ObjectMapper objectMapper, @Value("${file.output-dir}") String outputDir) {
        this.objectMapper = objectMapper;
        this.outputDir = Path.of(outputDir);
    }

    public void process(String json) throws IOException {
        ObjectNode root = (ObjectNode) objectMapper.readTree(json);
        String filename = root.path("filename").asText();
        if (filename.isBlank()) {
            throw new IOException("Kafka message did not contain a 'filename' field: " + json);
        }

        ArrayNode logging = root.has("logging") && root.get("logging").isArray()
                ? (ArrayNode) root.get("logging")
                : root.putArray("logging");
        ObjectNode logEntry = objectMapper.createObjectNode();
        logEntry.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        logEntry.put("message", RECEIVED_LOG_MESSAGE);
        logging.add(logEntry);

        String updatedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);

        Files.createDirectories(outputDir);
        Path target = outputDir.resolve(filename);
        Files.writeString(target, updatedJson, StandardCharsets.UTF_8);
        log.info("Wrote message for '{}' to '{}'", filename, target);
    }
}
