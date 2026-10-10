package blinkstay.common.events;

import java.util.UUID;

public record ListingSuspendEvent(UUID listingId, UUID managerId, String title, String reason) {

}
