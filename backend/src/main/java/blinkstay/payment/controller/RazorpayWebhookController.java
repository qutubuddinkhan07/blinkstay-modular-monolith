package blinkstay.payment.controller;

import blinkstay.payment.enums.ProviderEventType;
import blinkstay.payment.gateway.ProviderEvent;
import blinkstay.payment.gateway.RazorpaySignature;
import blinkstay.payment.service.PaymentEventProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v5/payments/webhook")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "razorpay")
public class RazorpayWebhookController {
    private final PaymentEventProcessor processor;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.payment.razorpay.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/razorpay")
    public ResponseEntity<String> handle(@RequestBody byte[] body,
                                         @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                         @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {
        if (!RazorpaySignature.valid(body, signature, webhookSecret)) {
            log.warn("Razorpay webhook rejected: bad signature");
            return ResponseEntity.status(401).body("invalid signature");
        }

        try {
            JsonNode root = mapper.readTree(new String(body, StandardCharsets.UTF_8));
            String type = root.path("event").asText("");
            JsonNode pay = root.path("payload").path("payment").path("entity");
            String orderId = pay.path("order_id").asText(null);
            String paymentId = pay.path("id").asText(null);

            ProviderEventType eventType = switch (type) {
                case "payment.captured" -> ProviderEventType.PAYMENT_SUCCEEDED;
                case "payment.failed" -> ProviderEventType.PAYMENT_FAILED;
                default -> null;
            };
            if (eventType == null || orderId == null || paymentId == null) {
                return ResponseEntity.ok("ignored"); // answer 200 so Razorpay does not retry events we do not use
            }

            BigDecimal amount = BigDecimal.valueOf(pay.path("amount").asLong()).movePointLeft(2); // paise -> rupees
            String currency = pay.path("currency").asText("INR");
            String id = eventId != null ? eventId : type + "_" + paymentId;
            String reason = pay.path("error_description").asText(null);

            processor.process("RAZORPAY",
                    new ProviderEvent(id, eventType, orderId, paymentId, amount, currency, reason));
            return ResponseEntity.ok("ok");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return ResponseEntity.badRequest().body("bad payload");
        }
        // any other exception becomes a 500 on purpose: Razorpay retries, and the processor is idempotent
    }
}