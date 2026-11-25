package blinkstay.support;

import blinkstay.auth.config.SpringSecurityConfiguration;
import blinkstay.auth.controller.AuthController;
import blinkstay.auth.util.JWTUtil;
import blinkstay.booking.controller.BookingController;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Web + security slice: MVC, the real security chain (SpringSecurityConfiguration,
 * JWTFilter, JWTUtil), GlobalExceptionHandler and two real controllers. No database.
 * Every class with this annotation shares one Spring context.
 * Use together with SecurityWebTestSupport (mocks and helpers).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WebMvcTest(controllers = {AuthController.class, BookingController.class})
@ActiveProfiles("test")
@Import({SpringSecurityConfiguration.class, JWTUtil.class})
public @interface SecurityWebTest {
}
