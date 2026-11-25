package blinkstay.support;

import blinkstay.auth.security.AuthUser.AuthUser;
import blinkstay.auth.service.AuthService;
import blinkstay.auth.service.BlockedTokenService;
import blinkstay.auth.serviceImpl.CustomUserDetailsDaoService;
import blinkstay.auth.util.JWTUtil;
import blinkstay.booking.service.BookingCancellationService;
import blinkstay.booking.service.BookingService;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;

/**
 * Mocks and helpers shared by every @SecurityWebTest class (same mocks = one shared context).
 */
public abstract class SecurityWebTestSupport {

    protected static final String AUTH_COOKIE = "BLINKSTAY_TOKEN";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JWTUtil jwtUtil;

    // used by JWTFilter
    @MockitoBean
    protected CustomUserDetailsDaoService userDetailsService;

    @MockitoBean
    protected BlockedTokenService blockedTokenService;

    // used by the two controllers in the slice
    @MockitoBean
    protected AuthService authService;

    @MockitoBean
    protected BookingService bookingService;

    @MockitoBean
    protected BookingCancellationService cancellationService;

    /**
     * A real, signed login cookie for a new user with these roles.
     */
    protected Cookie loginAs(String... roles) {
        return loginAs(UUID.randomUUID(), roles);
    }

    /**
     * A real, signed login cookie; the filter will find this user as active with these roles.
     */
    protected Cookie loginAs(UUID userId, String... roles) {
        when(userDetailsService.loadAuthSnapshot(userId.toString()))
                .thenReturn(authUser(userId, true, null, roles));
        return authCookie(jwtUtil.generateToken(userId.toString(), List.of(roles)));
    }

    protected static AuthUser authUser(UUID userId, boolean enabled, Instant passwordChangedAt, String... roles) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        return new AuthUser(userId.toString(), "unused", enabled, authorities, passwordChangedAt);
    }

    protected static Cookie authCookie(String token) {
        return new Cookie(AUTH_COOKIE, token);
    }

    protected static final String CSRF_COOKIE = "XSRF-TOKEN";
    protected static final String CSRF_HEADER = "X-XSRF-TOKEN";

    /**
     * What the React app does: the same token in the XSRF-TOKEN cookie and the X-XSRF-TOKEN header.
     */
    protected static RequestPostProcessor spaCsrf() {
        return spaCsrf("test-csrf-token", "test-csrf-token");
    }

    /**
     * Cookie and header values chosen by the test (different values = forged request).
     */
    protected static RequestPostProcessor spaCsrf(String cookieValue, String headerValue) {
        return request -> {
            Cookie xsrf = new Cookie(CSRF_COOKIE, cookieValue);
            Cookie[] existing = request.getCookies();
            request.setCookies(existing == null ? new Cookie[]{xsrf}
                    : Stream.concat(Stream.of(existing), Stream.of(xsrf)).toArray(Cookie[]::new));
            request.addHeader(CSRF_HEADER, headerValue);
            return request;
        };
    }
}