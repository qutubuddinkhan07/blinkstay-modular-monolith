package blinkstay.listing.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import blinkstay.listing.entities.Listing;
import blinkstay.listing.enums.ListingStatus;
import blinkstay.listing.enums.SuspensionSource;
import blinkstay.listing.repository.ListingRepository;
import blinkstay.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingModerationService {
	private final ListingRepository listingRepo;

	private final RoomService roomService;

	private final CacheManager cacheManager;

	/**
	 * Called when a host is blocked: every non-suspended listing becomes SUSPENDED
	 * (OWNER_BLOCKED).
	 */
	@Transactional
	public int suspendAllByManager(UUID managerId, String reason) {
		List<Listing> listings = listingRepo.findByManagerIdAndStatusNot(managerId, ListingStatus.SUSPENDED);
		LocalDateTime now = LocalDateTime.now();

		for (Listing l : listings) {
			l.setStatusBeforeSuspension(l.getStatus());
			l.setStatus(ListingStatus.SUSPENDED);
			l.setSuspensionSource(SuspensionSource.OWNER_BLOCKED);
			l.setSuspensionReason(reason);
			l.setSuspendedAt(now);
		}
		listingRepo.saveAll(listings);
		evictListingCaches();

		log.info("Suspended {} listings of blocked manager {}", listings.size(), managerId);
		return listings.size();
	}

	/**
	 * Called when a host is unblocked: only listings suspended BY THE BLOCK are
	 * restored.
	 */
	@Transactional
	public int restoreAfterUnblock(UUID managerId) {
		List<Listing> listings = listingRepo.findByManagerIdAndStatusAndSuspensionSource(managerId,
				ListingStatus.SUSPENDED, SuspensionSource.OWNER_BLOCKED);

		for (Listing l : listings) {
			ListingStatus restore = l.getStatusBeforeSuspension() != null ? l.getStatusBeforeSuspension()
					: ListingStatus.DRAFT;

			// a listing that lost its last room while suspended must not go live empty
			if (restore == ListingStatus.PUBLISHED && !roomService.checkDoesListingHaveRooms(l.getId())) {
				restore = ListingStatus.DRAFT;
			}

			l.setStatus(restore);
			l.setStatusBeforeSuspension(null);
			l.setSuspensionSource(null);
			l.setSuspensionReason(null);
			l.setSuspendedAt(null);
		}
		listingRepo.saveAll(listings);
		evictListingCaches();

		log.info("Restored {} listings of unblocked manager {}", listings.size(), managerId);
		return listings.size();
	}

	/**
	 * Clears the listing caches AFTER the transaction commits. Evicting earlier
	 * lets a concurrent request re-cache the old data before the commit lands.
	 */
	private void evictListingCaches() {
		Runnable evict = () -> {
			clear("listingById");
			clear("publishedListings");
		};

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					evict.run();
				}
			});
		} else {
			evict.run();
		}
	}

	private void clear(String name) {
		Cache cache = cacheManager.getCache(name);
		if (cache != null) {
			cache.clear();
		}
	}
}
