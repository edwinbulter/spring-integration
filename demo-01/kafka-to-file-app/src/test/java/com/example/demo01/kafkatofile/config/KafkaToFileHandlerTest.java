package com.example.demo01.kafkatofile.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaToFileHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void appendsLogEntryToExistingLoggingArray() throws IOException {
        KafkaToFileHandler handler = new KafkaToFileHandler(objectMapper, tempDir.toString());
        String json = """
                {
                  "payload": "hello world\\n",
                  "filename": "20260823-194000-test.json",
                  "logging": [
                    { "timestamp": "2026-08-23 19:40:00", "message": "file processed from folder input-01" }
                  ]
                }
                """;

        handler.process(json);

        JsonNode written = readOutputFile("20260823-194000-test.json");
        assertThat(written.path("logging")).hasSize(2);
        assertThat(written.path("logging").get(0).path("message").asText())
                .isEqualTo("file processed from folder input-01");
        assertThat(written.path("logging").get(1).path("message").asText())
                .isEqualTo("message received from topic-01");
    }

    @Test
    void createsLoggingArrayWhenMissing() throws IOException {
        KafkaToFileHandler handler = new KafkaToFileHandler(objectMapper, tempDir.toString());
        String json = """
                {
                  "payload": "hello world\\n",
                  "filename": "20260823-194000-test.json"
                }
                """;

        handler.process(json);

        JsonNode written = readOutputFile("20260823-194000-test.json");
        assertThat(written.path("logging")).hasSize(1);
        assertThat(written.path("logging").get(0).path("message").asText())
                .isEqualTo("message received from topic-01");
    }

    @Test
    void throwsWhenFilenameIsMissing() {
        KafkaToFileHandler handler = new KafkaToFileHandler(objectMapper, tempDir.toString());
        String json = """
                { "payload": "hello world\\n" }
                """;

        assertThatThrownBy(() -> handler.process(json)).isInstanceOf(IOException.class);
    }

    private JsonNode readOutputFile(String filename) throws IOException {
        String content = Files.readString(tempDir.resolve(filename), StandardCharsets.UTF_8);
        return objectMapper.readTree(content);
    }
}
