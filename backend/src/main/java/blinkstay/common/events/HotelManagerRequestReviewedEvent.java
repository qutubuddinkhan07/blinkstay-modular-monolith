package blinkstay.common.events;

import java.util.UUID;

public record HotelManagerRequestReviewedEvent(UUID requestId, UUID userId, boolean approved, String reason) {

}
