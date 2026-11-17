package blinkstay.notification.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Sends email through the Zoho Mail REST API over HTTPS (port 443), for hosts that
 * block mail ports (Render's free plan blocks 25, 465 and 587).
 * The access token lives about 1 hour; it is renewed from the refresh token and kept in memory.
 */
@Component
@Slf4j
public class ZohoMailApiClient {
    private static final Duration RENEW_EARLY = Duration.ofMinutes(5);

    private final RestClient http = RestClient.create();

    @Value("${app.mail.zoho.accounts-url:https://accounts.zoho.in}")
    private String accountsUrl;

    @Value("${app.mail.zoho.api-url:https://mail.zoho.in}")
    private String apiUrl;

    @Value("${app.mail.zoho.client-id:}")
    private String clientId;

    @Value("${app.mail.zoho.client-secret:}")
    private String clientSecret;

    @Value("${app.mail.zoho.refresh-token:}")
    private String refreshToken;

    @Value("${app.mail.zoho.account-id:}")
    private String accountId;

    private String accessToken;
    private Instant expiresAt = Instant.EPOCH;

    /**
     * Throws on failure; the caller logs it.
     */
    public void send(String from, String to, String subject, String body, boolean html) {
        try {
            post(token(false), from, to, subject, body, html);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.UNAUTHORIZED) {
                throw e;
            }
            // token was revoked or expired early: get a fresh one and try once more
            post(token(true), from, to, subject, body, html);
        }
    }

    private void post(String token, String from, String to, String subject, String body, boolean html) {
        Map<String, Object> payload = Map.of("fromAddress", from, "toAddress", to, "subject", subject, "content",
                body, "mailFormat", html ? "html" : "plaintext");

        http.post().uri(apiUrl + "/api/accounts/{accountId}/messages", accountId)
                .header(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + token)
                .contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().toBodilessEntity();
    }

    // synchronized: several async email threads may need a token at the same moment
    private synchronized String token(boolean force) {
        if (!force && accessToken != null && Instant.now().isBefore(expiresAt)) {
            return accessToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);

        Map<?, ?> res = http.post().uri(accountsUrl + "/oauth/v2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(Map.class);

        // Zoho answers 200 with {"error": "..."} when the refresh fails
        Object token = res == null ? null : res.get("access_token");
        if (token == null) {
            throw new IllegalStateException(
                    "Zoho token refresh failed: " + (res == null ? "empty response" : res.get("error")));
        }
        long seconds = res.get("expires_in") instanceof Number n ? n.longValue() : 3600;

        accessToken = token.toString();
        expiresAt = Instant.now().plusSeconds(seconds).minus(RENEW_EARLY);
        log.info("Zoho Mail access token renewed");
        return accessToken;
    }
}