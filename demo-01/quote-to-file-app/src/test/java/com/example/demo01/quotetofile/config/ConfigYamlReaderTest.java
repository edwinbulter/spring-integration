package com.example.demo01.quotetofile.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigYamlReaderTest {

    private static final long DEFAULT_PERIOD_SECONDS = 30;

    @TempDir
    Path tempDir;

    @Test
    void returnsConfiguredPeriodWhenFileIsValid() throws IOException {
        Path configFile = writeConfig("QuoteToFile:\n  periodInSeconds: 5\n");
        ConfigYamlReader reader = new ConfigYamlReader(configFile.toString(), DEFAULT_PERIOD_SECONDS);

        assertThat(reader.getPeriodInSeconds()).isEqualTo(5);
    }

    @Test
    void fallsBackToDefaultWhenFileIsMissing() {
        Path missingFile = tempDir.resolve("does-not-exist.yml");
        ConfigYamlReader reader = new ConfigYamlReader(missingFile.toString(), DEFAULT_PERIOD_SECONDS);

        assertThat(reader.getPeriodInSeconds()).isEqualTo(DEFAULT_PERIOD_SECONDS);
    }

    @Test
    void fallsBackToDefaultWhenPeriodIsNotNumeric() throws IOException {
        Path configFile = writeConfig("QuoteToFile:\n  periodInSeconds: \"not-a-number\"\n");
        ConfigYamlReader reader = new ConfigYamlReader(configFile.toString(), DEFAULT_PERIOD_SECONDS);

        assertThat(reader.getPeriodInSeconds()).isEqualTo(DEFAULT_PERIOD_SECONDS);
    }

    @Test
    void fallsBackToDefaultWhenSectionIsMissing() throws IOException {
        Path configFile = writeConfig("SomeOtherSection:\n  foo: bar\n");
        ConfigYamlReader reader = new ConfigYamlReader(configFile.toString(), DEFAULT_PERIOD_SECONDS);

        assertThat(reader.getPeriodInSeconds()).isEqualTo(DEFAULT_PERIOD_SECONDS);
    }

    @Test
    void fallsBackToDefaultWhenYamlIsMalformed() throws IOException {
        Path configFile = writeConfig("this: [is not, valid: yaml");
        ConfigYamlReader reader = new ConfigYamlReader(configFile.toString(), DEFAULT_PERIOD_SECONDS);

        assertThat(reader.getPeriodInSeconds()).isEqualTo(DEFAULT_PERIOD_SECONDS);
    }

    private Path writeConfig(String content) throws IOException {
        Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, content);
        return configFile;
    }
}
