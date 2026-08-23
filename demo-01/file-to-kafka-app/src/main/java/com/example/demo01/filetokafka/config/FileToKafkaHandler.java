package com.example.demo01.filetokafka.config;

import com.example.demo01.filetokafka.model.FileMessage;
import com.example.demo01.filetokafka.model.LogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Reads a single file, wraps its content in a JSON message, publishes it to Kafka
 * and, once the send succeeded, moves the original file to the processed folder.
 */
@Component
public class FileToKafkaHandler {

    private static final Logger log = LoggerFactory.getLogger(FileToKafkaHandler.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILENAME_PREFIX_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final Path processedDir;

    public FileToKafkaHandler(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               @Value("${kafka.topic}") String topic,
                               @Value("${file.processed-dir}") String processedDir) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.processedDir = Path.of(processedDir);
    }

    public void process(File file) throws IOException {
        String content = java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
        String timestampPrefix = LocalDateTime.now().format(FILENAME_PREFIX_FORMAT);
        String baseName = stripExtension(file.getName());
        // The file moved into processed-01 keeps its original extension (its content
        // is not JSON); only the filename referenced in the JSON message / topic key
        // (and therefore the file written by kafka-to-file-app) gets a .json extension.
        String processedFilename = timestampPrefix + "-" + file.getName();
        String jsonFilename = timestampPrefix + "-" + baseName + ".json";

        LogEntry logEntry = new LogEntry(LocalDateTime.now().format(TIMESTAMP_FORMAT),
                "file processed from folder input-01");
        FileMessage fileMessage = new FileMessage(content, jsonFilename, List.of(logEntry));

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fileMessage);
        try {
            kafkaTemplate.send(topic, jsonFilename, json).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while sending message to Kafka topic " + topic, e);
        } catch (ExecutionException e) {
            throw new IOException("Failed to send message to Kafka topic " + topic, e);
        }

        java.nio.file.Files.createDirectories(processedDir);
        Path target = processedDir.resolve(processedFilename);
        java.nio.file.Files.move(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Processed '{}' -> topic '{}' (as '{}') -> moved to '{}'", file.getName(), topic, jsonFilename, target);
    }

    /**
     * Strips the extension (if any) off a filename, e.g. "test.txt" -> "test".
     */
    private static String stripExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }
}
