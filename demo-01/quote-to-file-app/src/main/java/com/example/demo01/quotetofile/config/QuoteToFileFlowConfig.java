package com.example.demo01.quotetofile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.dsl.Files;

import java.io.File;

@Configuration
public class QuoteToFileFlowConfig {

    @Bean
    public IntegrationFlow quoteToFileFlow(QuoteMessageSource quoteMessageSource,
                                            @Value("${quote.input-dir}") String inputDir) {
        return IntegrationFlow
                .from(quoteMessageSource, e -> e.poller(Pollers.fixedDelay(1000)))
                .handle(Files.outboundAdapter(new File(inputDir)).autoCreateDirectory(true))
                .get();
    }
}
