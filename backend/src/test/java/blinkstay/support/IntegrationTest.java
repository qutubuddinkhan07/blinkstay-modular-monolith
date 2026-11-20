package blinkstay.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;


/**
 * Put this on any test class that needs the whole Spring application
 * and a real PostgreSQL. Name such classes *IT (run by "mvn verify").
 *
 * @IntegrationTest class BookingFlowIT { ... }
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresContainerConfig.class)
public @interface IntegrationTest {
}
