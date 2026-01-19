package com.football501;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test to verify Spring Boot application context loads correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
class Football501ApplicationTest {

    @Test
    void contextLoads() {
        // If this test passes, Spring Boot setup is correct
        assertTrue(true);
    }
}
