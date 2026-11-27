package com.blinkstay.auth.config;

import com.blinkstay.support.SecurityWebTest;
import com.blinkstay.support.SecurityWebTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SecurityWebTest
@DisplayName("CSRF and CORS protection")
class CsrfAndCorsTest extends SecurityWebTestSupport {

    private static final String LOGIN_BODY = "{\"username\":\"guest@example.com\",\"password\":\"test-password\"}";

    @Nested
    @DisplayName("CSRF (double-submit cookie: XSRF-TOKEN cookie + X-XSRF-TOKEN header)")
    class Csrf {

        @Test
        void post_withoutCsrfToken_isRejectedWithCsrfInvalid() throws Exception {
            mvc.perform(post("/api/v5/bookings").cookie(loginAs("USER"))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        }

        @Test
        void post_whereHeaderDoesNotMatchCookie_isRejectedWithCsrfInvalid() throws Exception {
            mvc.perform(post("/api/v5/bookings").cookie(loginAs("USER"))
                            .with(spaCsrf("token-in-cookie", "forged-token-in-header"))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        }

        @Test
        void post_withMatchingCookieAndHeader_passesTheCsrfCheck() throws Exception {
            mvc.perform(post("/api/v5/bookings").cookie(loginAs("USER"))
                            .with(spaCsrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().is(not(403)));
        }

        @Test
        @DisplayName("a logged-out POST without a token gets 403 CSRF_INVALID, not 401 (CSRF is checked first)")
        void anonymousPost_withoutCsrfToken_getsCsrfErrorBeforeLoginCheck() throws Exception {
            mvc.perform(post("/api/v5/bookings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        }

        @Test
        void get_needsNoCsrfToken() throws Exception {
            mvc.perform(get("/api/v5/bookings/me").cookie(loginAs("USER")))
                    .andExpect(status().isOk());
        }

        @Test
        void login_needsNoCsrfToken() throws Exception {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                    .andExpect(status().isOk());
        }

        @Test
        void logout_needsNoCsrfTokenAndNoLogin() throws Exception {
            mvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isOk());
        }

        @Test
        void razorpayWebhook_needsNoCsrfToken() throws Exception {
            mvc.perform(post("/api/v5/payments/webhook/razorpay")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().is(not(403)));
        }

        @Test
        @DisplayName("every response hands the frontend an XSRF-TOKEN cookie (csrf.spa() loads the token eagerly)")
        void anyResponse_setsTheXsrfTokenCookie() throws Exception {
            mvc.perform(get("/api/v3/listings/all"))
                    .andExpect(cookie().exists(CSRF_COOKIE));
        }

        @Test
        @DisplayName("a POST refused for a missing token hands out a fresh XSRF-TOKEN cookie, so the app can retry")
        void postRefusedForCsrf_handsOutAFreshXsrfCookie() throws Exception {
            mvc.perform(post("/api/v5/bookings").cookie(loginAs("USER"))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(cookie().exists(CSRF_COOKIE));
        }
    }

    @Nested
    @DisplayName("CORS (browser preflight)")
    class Cors {

        @Test
        void preflightFromTheFrontendOrigin_isAllowedWithCredentials() throws Exception {
            mvc.perform(options("/api/v5/bookings/me")
                            .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
        }

        @Test
        void preflightFromAnUnknownOrigin_isRejected() throws Exception {
            mvc.perform(options("/api/v5/bookings/me")
                            .header(HttpHeaders.ORIGIN, "https://evil.example")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                    .andExpect(status().isForbidden());
        }
    }
}