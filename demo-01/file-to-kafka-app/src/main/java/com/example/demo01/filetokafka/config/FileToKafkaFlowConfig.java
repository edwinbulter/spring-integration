package com.example.demo01.filetokafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;
import org.springframework.messaging.MessagingException;

import java.io.File;
import java.io.IOException;

@Configuration
public class FileToKafkaFlowConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public IntegrationFlow fileToKafkaFlow(FileToKafkaHandler handler,
                                            @Value("${file.input-dir}") String inputDir,
                                            @Value("${file.poll-delay-ms}") long pollDelayMs) {
        return IntegrationFlow
                .from(Files.inboundAdapter(new File(inputDir))
                                .autoCreateDirectory(true)
                                // Files are moved out of input-01 once processed, so there is no
                                // need to remember filenames forever (the default filter would
                                // otherwise silently ignore a re-submitted file with the same name).
                                .filter(new IgnoreHiddenFileListFilter()),
                        e -> e.poller(Pollers.fixedDelay(pollDelayMs)))
                .<File>handle((file, headers) -> {
                    try {
                        handler.process(file);
                    } catch (IOException e) {
                        throw new MessagingException("Failed to process file " + file, e);
                    }
                    return null;
                })
                .get();
    }
}
