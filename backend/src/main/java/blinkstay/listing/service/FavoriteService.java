package blinkstay.listing.service;

import java.util.List;
import java.util.UUID;

import blinkstay.listing.dto.PagedResponse;
import blinkstay.listing.dto.PublishedListingDto;

public interface FavoriteService {
	void add(UUID userId, UUID listingId);

	void remove(UUID userId, UUID listingId);

	PagedResponse<PublishedListingDto> list(UUID userId, int page, int size);

	List<UUID> ids(UUID userId);

	void removeAllForUser(UUID userId);
}
