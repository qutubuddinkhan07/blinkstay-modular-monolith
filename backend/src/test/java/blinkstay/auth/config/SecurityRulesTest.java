package blinkstay.auth.config;

import blinkstay.support.SecurityWebTest;
import blinkstay.support.SecurityWebTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The URL rules in SpringSecurityConfiguration, one row per rule.
 * ALLOWED = security let the request through (the answer is whatever the controller,
 * or a 404/405 for controllers not loaded in this slice, says, but never 401 or 403).
 * Every request carries a valid CSRF token, so only login and roles are tested here.
 */
@SecurityWebTest
@DisplayName("Security URL rules: who may call what")
class SecurityRulesTest extends SecurityWebTestSupport {

    private static final String ID = "00000000-0000-0000-0000-000000000001";

    @ParameterizedTest(name = "{0} {1} as {2} -> {3}")
    @CsvSource({
            // public
            "GET,    /api/v3/listings/all,                    ANONYMOUS,     ALLOWED",
            "GET,    /api/v3/listings/" + ID + ",             ANONYMOUS,     ALLOWED",
            "GET,    /api/v5/listings/" + ID + "/reviews,     ANONYMOUS,     ALLOWED",
            "GET,    /api/v5/availability/" + ID + ",         ANONYMOUS,     ALLOWED",
            "POST,   /api/v1/auth/login,                      ANONYMOUS,     ALLOWED",
            "POST,   /api/v1/auth/logout,                     ANONYMOUS,     ALLOWED",
            "POST,   /api/v5/payments/webhook/razorpay,       ANONYMOUS,     ALLOWED",
            // any logged-in user
            "GET,    /api/v5/bookings/me,                     ANONYMOUS,     401",
            "GET,    /api/v5/bookings/me,                     USER,          ALLOWED",
            "GET,    /api/v2/user/me,                         ANONYMOUS,     401",
            "GET,    /api/v2/user/me,                         USER,          ALLOWED",
            "GET,    /api/v2/notifications,                   USER,          ALLOWED",
            "GET,    /api/v9/no-such-endpoint,                ANONYMOUS,     401",
            // hosts
            "GET,    /api/v3/listings/my-listings,            ANONYMOUS,     401",
            "GET,    /api/v3/listings/my-listings,            USER,          403",
            "GET,    /api/v3/listings/my-listings,            HOTEL_MANAGER, ALLOWED",
            "POST,   /api/v3/listings/create,                 USER,          403",
            "POST,   /api/v3/listings/create,                 HOTEL_MANAGER, ALLOWED",
            "GET,    /api/v5/host/bookings,                   USER,          403",
            "GET,    /api/v5/host/bookings,                   ADMIN,         403",
            "GET,    /api/v5/host/bookings,                   HOTEL_MANAGER, ALLOWED",
            "POST,   /api/v4/rooms/" + ID + "/rooms,          ANONYMOUS,     401",
            // admins
            "GET,    /api/v2/admin/users,                     HOTEL_MANAGER, 403",
            "GET,    /api/v2/admin/users,                     ADMIN,         ALLOWED",
            "GET,    /api/v5/admin/bookings,                  USER,          403",
            "GET,    /api/v5/admin/bookings,                  ADMIN,         ALLOWED",
            "POST,   /api/v3/admin/listings/" + ID + "/suspend, HOTEL_MANAGER, 403",
            "DELETE, /api/v2/admin/cache/usersById,           USER,          403"
    })
    void urlRule(String method, String path, String role, String expected) throws Exception {
        MockHttpServletRequestBuilder request = request(HttpMethod.valueOf(method), path).with(spaCsrf());
        if (!"ANONYMOUS".equals(role)) {
            request.cookie(loginAs(role));
        }

        ResultActions result = mvc.perform(request);

        switch (expected) {
            case "ALLOWED" -> result.andExpect(status().is(not(anyOf(equalTo(401), equalTo(403)))));
            case "401" -> result.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
            case "403" -> result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
            default -> throw new IllegalArgumentException("Unknown expectation " + expected);
        }
    }
}