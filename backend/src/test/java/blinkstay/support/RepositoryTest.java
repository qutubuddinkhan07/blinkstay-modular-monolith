package blinkstay.support;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Repository test on the real Postgres container: only JPA is started, and every
 * test is rolled back at the end, so each one starts with empty tables.
 * Classes using it end in IT (they need Docker).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({PostgresContainerConfig.class, NoOpCacheConfig.class})
public @interface RepositoryTest {
}
