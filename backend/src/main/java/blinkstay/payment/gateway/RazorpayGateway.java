package blinkstay.payment.gateway;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "razorpay")
public class RazorpayGateway implements PaymentGateway {
    private final RazorpayClient client;

    public RazorpayGateway(@Value("${app.payment.razorpay.key-id}") String keyId,
                           @Value("${app.payment.razorpay.key-secret}") String keySecret) throws RazorpayException {
        this.client = new RazorpayClient(keyId, keySecret);
    }

    @Override
    public String name() {
        return "RAZORPAY";
    }

    @Override
    public ProviderOrder createOrder(String reference, BigDecimal amount, String currency) {
        try {
            JSONObject req = new JSONObject();
            req.put("amount", toMinorUnits(amount));
            req.put("currency", currency);
            req.put("receipt", reference); // max 40 chars, ours is 12
            Order order = client.orders.create(req);
            String id = order.get("id");
            return new ProviderOrder(id);
        } catch (RazorpayException e) {
            log.warn("Razorpay createOrder failed for {}: {}", reference, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment provider is unavailable, try again");
        }
    }

    @Override
    public ProviderRefund refund(String providerPaymentId, BigDecimal amount, String reason) {
        try {
            long minor = toMinorUnits(amount);

            // already refunded earlier (for example the app crashed after Razorpay accepted it)? reuse it
            for (Refund existing : client.payments.fetchAllRefunds(providerPaymentId)) {
                Number existingAmount = existing.get("amount");
                String existingStatus = existing.get("status");
                if (existingAmount != null && existingAmount.longValue() == minor
                        && !"failed".equals(existingStatus)) {
                    String existingId = existing.get("id");
                    return new ProviderRefund(existingId);
                }
            }

            JSONObject req = new JSONObject();
            req.put("amount", minor);
            req.put("notes", new JSONObject().put("reason", reason == null ? "" : reason));
            Refund refund = client.payments.refund(providerPaymentId, req);
            String refundId = refund.get("id");
            return new ProviderRefund(refundId);
        } catch (RazorpayException e) {
            // the real reason is logged and kept in refund_error, so an admin can see why it failed
            log.warn("Razorpay refund failed for payment {} (amount {}): {}", providerPaymentId, amount,
                    e.getMessage(), e);
            String hint = e.getMessage() != null && e.getMessage().contains("invalid request sent")
                    ? " (Razorpay gave no reason; in test mode try the refund from the Razorpay dashboard)" : "";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Refund rejected by provider: " + shorten(e.getMessage()) + hint);
        }
        /**
         * if the balance is low on RAZORPAY then there will be no refund
         */
    }

    @Override
    public Optional<ProviderCapture> findCapturedPayment(String orderId) {
        try {
            for (com.razorpay.Payment pay : client.orders.fetchPayments(orderId)) {
                String status = pay.get("status");
                if ("captured".equals(status)) {
                    String id = pay.get("id");
                    Number amount = pay.get("amount");
                    String currency = pay.get("currency");
                    return Optional.of(new ProviderCapture(id,
                            BigDecimal.valueOf(amount.longValue()).movePointLeft(2), currency));
                }
            }
            return Optional.empty();
        } catch (RazorpayException e) {
            log.warn("Razorpay fetchPayments failed for order {}: {}", orderId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment provider is unavailable");
        }
    }

    private static String shorten(String message) {
        if (message == null || message.isBlank()) return "no details from provider";
        return message.length() > 150 ? message.substring(0, 150) : message;
    }

    // Razorpay wants the smallest unit: rupees -> paise
    private static long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }
}