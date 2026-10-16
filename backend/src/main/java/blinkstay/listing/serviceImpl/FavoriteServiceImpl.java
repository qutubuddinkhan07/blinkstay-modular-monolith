package blinkstay.listing.serviceImpl;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import blinkstay.listing.dto.PagedResponse;
import blinkstay.listing.dto.PublishedListingDto;
import blinkstay.listing.entities.Favorite;
import blinkstay.listing.entities.Listing;
import blinkstay.listing.enums.ListingStatus;
import blinkstay.listing.repository.FavoriteRepository;
import blinkstay.listing.repository.ListingRepository;
import blinkstay.listing.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteServiceImpl implements FavoriteService {
	private static final int MAX_FAVORITES = 200;

	private final FavoriteRepository favoriteRepo;
	private final ListingRepository listingRepo;
	private final PublishedListingAssembler assembler;

	// Deliberately NOT @Transactional: each repository call has its own
	// transaction, so a
	// duplicate-row error from a parallel request can be caught here without
	// poisoning
	// a surrounding transaction.
	@Override
	public void add(UUID userId, UUID listingId) {
		listingRepo.findById(listingId).filter(l -> l.getStatus() == ListingStatus.PUBLISHED)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not available"));

		if (favoriteRepo.existsByUserIdAndListingId(userId, listingId)) {
			return;
		}

		if (favoriteRepo.countByUserId(userId) >= MAX_FAVORITES) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"You can save up to " + MAX_FAVORITES + " fovorites. Remove one first.");
		}

		try {
			favoriteRepo.saveAndFlush(Favorite.builder().userId(userId).listingId(listingId).build());
		} catch (DataIntegrityViolationException e) {
			// a parallel request added it a moment ago: the unique constraint let only one
			// through
		}
	}

	@Override
	@Transactional
	public void remove(UUID userId, UUID listingId) {
		favoriteRepo.deleteOne(userId, listingId); // removing something that is not there is fine
	}

	@Override
	@Transactional(readOnly = true)
	public PagedResponse<PublishedListingDto> list(UUID userId, int page, int size) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
		Page<Listing> result = favoriteRepo.findPublishedFavorites(userId, ListingStatus.PUBLISHED, pageable);

		List<PublishedListingDto> cards = assembler.assemble(result.getContent());
		return PagedResponse.from(new PageImpl<>(cards, pageable, result.getTotalElements()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<UUID> ids(UUID userId) {
		return favoriteRepo.findListingIdsByUserId(userId);
	}

	@Override
	@Transactional
	public void removeAllForUser(UUID userId) {
		favoriteRepo.deleteAllForUser(userId);
	}

}
