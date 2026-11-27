package com.blinkstay.auth.filter;

import com.blinkstay.support.SecurityWebTest;
import com.blinkstay.support.SecurityWebTestSupport;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SecurityWebTest
@DisplayName("JWTFilter: every reason a login cookie is refused (401 + code, cookie cleared)")
class JWTFilterTest extends SecurityWebTestSupport {

    private static final String PROTECTED = "/api/v5/bookings/me";
    private static final String OTHER_SECRET = "another-secret-that-is-long-enough-for-hs256-0123456789";

    @Value("${jwt.signature}")
    private String jwtSignature;

    private final UUID userId = UUID.randomUUID();

    // ---------- helpers ----------

    /**
     * A token built by hand, so tests can choose the dates and the signing key.
     */
    private String token(Instant issuedAt, Instant expiresAt, String secret) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String expiredToken() {
        Instant now = Instant.now();
        return token(now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1)), jwtSignature);
    }

    private ResultActions getProtected(Cookie cookie) throws Exception {
        return mvc.perform(get(PROTECTED).cookie(cookie));
    }

    /**
     * The login cookie is deleted in the browser: same name, empty value, Max-Age=0.
     */
    private static ResultMatcher authCookieCleared() {
        return header().stringValues(HttpHeaders.SET_COOKIE,
                hasItem(allOf(startsWith(AUTH_COOKIE + "="), containsString("Max-Age=0"))));
    }

    /**
     * 401 + the reason code + the login cookie deleted, checked as one matcher.
     */
    private static ResultMatcher rejectedWith(String code) {
        return result -> {
            status().isUnauthorized().match(result);
            jsonPath("$.code").value(code).match(result);
            authCookieCleared().match(result);
        };
    }

    // ---------- accepted ----------

    @Test
    void validCookie_reachesTheControllerAsThatUser() throws Exception {
        getProtected(loginAs(userId, "USER")).andExpect(status().isOk());

        verify(bookingService).myBookings(eq(userId), any(), anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void tokenIssuedAfterThePasswordChange_isAccepted() throws Exception {
        Cookie cookie = loginAs(userId, "USER");
        when(userDetailsService.loadAuthSnapshot(userId.toString()))
                .thenReturn(authUser(userId, true, Instant.now().minus(Duration.ofHours(1)), "USER"));

        getProtected(cookie).andExpect(status().isOk());
    }

    // ---------- refused ----------

    @Test
    void noCookie_isUnauthenticated() throws Exception {
        mvc.perform(get(PROTECTED))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void garbageToken_isTokenInvalid() throws Exception {
        getProtected(authCookie("not-a-jwt")).andExpect(rejectedWith("TOKEN_INVALID"));
    }

    @Test
    void tokenSignedWithAnotherKey_isTokenInvalid() throws Exception {
        Instant now = Instant.now();
        String forged = token(now, now.plus(Duration.ofHours(1)), OTHER_SECRET);

        getProtected(authCookie(forged)).andExpect(rejectedWith("TOKEN_INVALID"));
    }

    @Test
    void expiredToken_isTokenExpired() throws Exception {
        getProtected(authCookie(expiredToken())).andExpect(rejectedWith("TOKEN_EXPIRED"));
    }

    @Test
    void loggedOutToken_isTokenRevoked() throws Exception {
        Cookie cookie = loginAs(userId, "USER");
        when(blockedTokenService.checkIfPresent(cookie.getValue())).thenReturn(true);

        getProtected(cookie).andExpect(rejectedWith("TOKEN_REVOKED"));
    }

    @Test
    void blockedAccount_isAccountBlocked() throws Exception {
        Cookie cookie = loginAs(userId, "USER");
        when(userDetailsService.loadAuthSnapshot(userId.toString()))
                .thenReturn(authUser(userId, false, null, "USER"));

        getProtected(cookie).andExpect(rejectedWith("ACCOUNT_BLOCKED"));
    }

    @Test
    void tokenIssuedBeforeThePasswordChange_isPasswordChanged() throws Exception {
        Cookie cookie = loginAs(userId, "USER");
        when(userDetailsService.loadAuthSnapshot(userId.toString()))
                .thenReturn(authUser(userId, true, Instant.now().plus(Duration.ofMinutes(1)), "USER"));

        getProtected(cookie).andExpect(rejectedWith("PASSWORD_CHANGED"));
    }

    @Test
    void deletedUser_isUserNotFound() throws Exception {
        Cookie cookie = loginAs(userId, "USER");
        when(userDetailsService.loadAuthSnapshot(userId.toString()))
                .thenThrow(new UsernameNotFoundException("User not found"));

        getProtected(cookie).andExpect(rejectedWith("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("a dead cookie never blocks a public page; it is only cleared")
    void deadCookieOnPublicPage_doesNotBlockThePage_butIsCleared() throws Exception {
        mvc.perform(get("/api/v3/listings/all").cookie(authCookie(expiredToken())))
                .andExpect(status().is(not(401)))
                .andExpect(authCookieCleared());
    }
}