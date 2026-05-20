package com.football501;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Base test class for integration tests that require a full Spring context.
 * Provides a wired MockMvc via @AutoConfigureMockMvc — subclasses inject it with @Autowired.
 * Unit tests that don't need Spring should NOT extend this class.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseTest {
}
