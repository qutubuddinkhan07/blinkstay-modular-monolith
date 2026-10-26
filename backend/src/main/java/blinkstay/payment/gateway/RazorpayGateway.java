package blinkstay.payment.gateway;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

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
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment provider is unavailable, try again");
        }
    }

    @Override
    public ProviderRefund refund(String providerPaymentId, BigDecimal amount, String reason) {
        try {
            JSONObject req = new JSONObject();
            req.put("amount", toMinorUnits(amount));
            req.put("notes", new JSONObject().put("reason", reason == null ? "" : reason));
            Refund refund = client.payments.refund(providerPaymentId, req);
            String refundId = refund.get("id");
            return new ProviderRefund(refundId);
        } catch (RazorpayException e) {
            // thrown so the surrounding transaction rolls back; stage 2 will move this out of the transaction
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Refund could not be sent to the provider");
        }
    }

    // Razorpay wants the smallest unit: rupees -> paise
    private static long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }
}
