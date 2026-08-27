package com.example.demo01.kafkatodb.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class KafkaToDbHandlerTest {

    private static final String INSERT_SQL = "INSERT INTO messages (filename, payload, creation_date) VALUES (?, ?, ?)";

    @Mock
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void insertsRowWithFilenameAndPayload() throws IOException {
        KafkaToDbHandler handler = new KafkaToDbHandler(jdbcTemplate, objectMapper);
        String json = """
                {
                  "payload": "hello world\\n",
                  "filename": "20260823-194000-test.json"
                }
                """;

        handler.process(json);

        ArgumentCaptor<Object> paramCaptor = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.eq(INSERT_SQL), paramCaptor.capture(), paramCaptor.capture(), paramCaptor.capture());

        assertThat(paramCaptor.getAllValues().get(0)).isEqualTo("20260823-194000-test.json");
        assertThat(paramCaptor.getAllValues().get(1)).isEqualTo("hello world\n");
        assertThat(paramCaptor.getAllValues().get(2)).isInstanceOf(Timestamp.class);
    }

    @Test
    void throwsAndDoesNotInsertWhenFilenameIsMissing() {
        KafkaToDbHandler handler = new KafkaToDbHandler(jdbcTemplate, objectMapper);
        String json = """
                { "payload": "hello world\\n" }
                """;

        assertThatThrownBy(() -> handler.process(json)).isInstanceOf(IOException.class);

        verifyNoInteractions(jdbcTemplate);
    }
}
