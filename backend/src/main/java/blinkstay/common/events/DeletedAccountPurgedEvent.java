package blinkstay.common.events;

import java.util.UUID;

/**
 * The account has been deleted for 30 days: its personal extras are being removed for good.
 */
public record DeletedAccountPurgedEvent(UUID userId) {
}