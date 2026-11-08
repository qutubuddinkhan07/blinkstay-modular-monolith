package blinkstay.common.stats;

public interface PaymentStats {
    /**
     * Payments an admin must look at: failed refunds and flagged payments.
     */
    long needingAttention();
}