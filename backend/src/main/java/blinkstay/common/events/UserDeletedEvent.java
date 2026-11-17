package blinkstay.common.events;

import java.util.UUID;

/**
 * host = the user was a host, so the email also tells them their listings are kept for 30 days.
 */
public record UserDeletedEvent(UUID userId, boolean host) {
}