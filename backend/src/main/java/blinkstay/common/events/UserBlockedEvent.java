package blinkstay.common.events;

import java.util.UUID;

public record UserBlockedEvent(UUID userId, String reason) {
}
