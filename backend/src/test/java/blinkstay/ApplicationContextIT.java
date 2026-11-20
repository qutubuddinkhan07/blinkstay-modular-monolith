package blinkstay;

import blinkstay.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test: starts the whole application against a real PostgreSQL.
 * Fails if any bean, required property or entity mapping is broken.
 */
@IntegrationTest
public class ApplicationContextIT {

    @Test
    @DisplayName("the whole application starts against a real PostgreSQL")
    void contextLoads() {
        // Nothing to assert: if Spring could not start, the test fails before this line.
    }
}
