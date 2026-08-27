package com.example.demo01.filetokafka.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileToKafkaHandlerTest {

    private static final String TOPIC = "topic-01";

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    private Path processedDir;

    @BeforeEach
    void setUp() {
        processedDir = tempDir.resolve("processed-01");
    }

    @Test
    void publishesJsonMessageAndMovesFileToProcessedDir() throws Exception {
        when(kafkaTemplate.send(eq(TOPIC), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        FileToKafkaHandler handler = new FileToKafkaHandler(kafkaTemplate, objectMapper, TOPIC, processedDir.toString());
        File inputFile = tempDir.resolve("test.txt").toFile();
        Files.writeString(inputFile.toPath(), "hello world\n", StandardCharsets.UTF_8);

        handler.process(inputFile);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), keyCaptor.capture(), jsonCaptor.capture());

        String jsonFilename = keyCaptor.getValue();
        assertThat(jsonFilename).endsWith("-test.json");

        JsonNode json = objectMapper.readTree(jsonCaptor.getValue());
        assertThat(json.path("payload").asText()).isEqualTo("hello world\n");
        assertThat(json.path("filename").asText()).isEqualTo(jsonFilename);
        assertThat(json.path("logging")).hasSize(1);
        assertThat(json.path("logging").get(0).path("message").asText())
                .isEqualTo("file processed from folder input-01");

        assertThat(inputFile).doesNotExist();
        List<Path> processedFiles = Files.list(processedDir).toList();
        assertThat(processedFiles).hasSize(1);
        assertThat(processedFiles.get(0).getFileName().toString()).endsWith("-test.txt");
    }

    @Test
    void doesNotMoveFileWhenKafkaSendFails() throws Exception {
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("Kafka unavailable"));
        when(kafkaTemplate.send(eq(TOPIC), anyString(), anyString())).thenReturn(failed);
        FileToKafkaHandler handler = new FileToKafkaHandler(kafkaTemplate, objectMapper, TOPIC, processedDir.toString());
        File inputFile = tempDir.resolve("test.txt").toFile();
        Files.writeString(inputFile.toPath(), "hello world\n", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> handler.process(inputFile))
                .isInstanceOf(IOException.class);

        assertThat(inputFile).exists();
    }
}
