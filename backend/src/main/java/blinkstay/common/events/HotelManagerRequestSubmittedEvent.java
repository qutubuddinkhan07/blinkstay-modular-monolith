package blinkstay.common.events;

import java.util.UUID;

public record HotelManagerRequestSubmittedEvent(UUID requestId, UUID userId, String username) {

}
