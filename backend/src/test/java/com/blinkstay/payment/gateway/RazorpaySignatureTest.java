package com.blinkstay.payment.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Webhook and verify signatures (Q61, Q62): HMAC-SHA256 of the exact bytes with the secret,
 * hex encoded. The expected values below were computed independently (Python hmac), so the
 * test does not re-use the code it is testing.
 */
class RazorpaySignatureTest {

    private static final String WEBHOOK_SECRET = "test_webhook_secret";
    private static final byte[] WEBHOOK_BODY = """
            {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_test_123","amount":400000}}}}"""
            .getBytes(StandardCharsets.UTF_8);
    private static final String WEBHOOK_SIGNATURE =
            "4cdeea9ea28e97f69187c1ce09f25272b192f4c6b7df91ea82bc3b2b686c7a43";

    @Test
    void correctSignature_isValid() {
        assertThat(RazorpaySignature.valid(WEBHOOK_BODY, WEBHOOK_SIGNATURE, WEBHOOK_SECRET)).isTrue();
    }

    @Test
    void checkoutVerify_orderIdPipePaymentId_isValid() {
        // The browser flow signs "orderId|paymentId" with the key secret
        byte[] payload = "order_test_1|pay_test_123".getBytes(StandardCharsets.UTF_8);
        String signature = "f67042e2b1dc5c03e539eec92d4d4547cbf0ecb840a03b056eed11470c14a97a";

        assertThat(RazorpaySignature.valid(payload, signature, "test_key_secret")).isTrue();
    }

    @Test
    void bodyChangedByOneCharacter_isInvalid() {
        // someone edits the amount after Razorpay signed it
        byte[] tampered = new String(WEBHOOK_BODY, StandardCharsets.UTF_8)
                .replace("400000", "100").getBytes(StandardCharsets.UTF_8);

        assertThat(RazorpaySignature.valid(tampered, WEBHOOK_SIGNATURE, WEBHOOK_SECRET)).isFalse();
    }

    @Test
    void wrongSecret_isInvalid() {
        assertThat(RazorpaySignature.valid(WEBHOOK_BODY, WEBHOOK_SIGNATURE, "another_secret")).isFalse();
    }

    @ParameterizedTest(name = "signature = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"not-a-signature"})
    void missingOrGarbageSignature_isInvalid(String signature) {
        assertThat(RazorpaySignature.valid(WEBHOOK_BODY, signature, WEBHOOK_SECRET)).isFalse();
    }

    @ParameterizedTest(name = "secret = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void missingOrBlankSecret_isInvalid_neverAccepted(String secret) {
        // a misconfigured server must reject everything, not accept everything
        assertThat(RazorpaySignature.valid(WEBHOOK_BODY, WEBHOOK_SIGNATURE, secret)).isFalse();
    }

    @Test
    void uppercaseHexSignature_isInvalid() {
        // Documents current behaviour: comparison is exact and Razorpay sends lowercase hex
        assertThat(RazorpaySignature.valid(WEBHOOK_BODY, WEBHOOK_SIGNATURE.toUpperCase(), WEBHOOK_SECRET))
                .isFalse();
    }
}