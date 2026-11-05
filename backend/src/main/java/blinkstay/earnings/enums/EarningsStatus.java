package blinkstay.earnings.enums;

public enum EarningsStatus {
    PENDING,    // booking is paid, the stay has not finished
    EARNED,     // the stay is over, the host is owed this amount
    REVERSED    // fully refunded, nothing is owed
}
