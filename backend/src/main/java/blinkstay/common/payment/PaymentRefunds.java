package blinkstay.common.payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentRefunds {
    /**
     * Locks the payment row. Call it before locking the booking row. Needs a running transaction.
     */
    void lockPayment(UUID paymentId);

    /**
     * Refunds the full amount of a succeeded payment and returns that amount. Needs a running transaction.
     */
    BigDecimal refundInFull(UUID paymentId, String reason);
}
