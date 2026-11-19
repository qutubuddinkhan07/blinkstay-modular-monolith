package blinkstay.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One real PostgreSQL 18 (same major version as Aiven) in Docker, for tests.
 *
 * @ServiceConnection hands the container's URL, user and password to Spring Boot,
 * so the test app never uses DB_URL from .env or the live database.
 * Spring caches the test context, so every integration test with the same
 * setup shares this one container instead of starting a new one.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18-alpine");
    }
}
