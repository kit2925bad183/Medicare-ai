package com.medicare;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ApplicationStartupIntegrationTest {

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void applicationStartsAndSerializesInstantsAsJsonStrings() throws Exception {
        assertEquals("\"2026-10-27T17:05:41Z\"",
            jsonMapper.writeValueAsString(Instant.parse("2026-10-27T17:05:41Z")));
    }
}
