package com.blinkstay.listing.serviceImpl;

import com.blinkstay.listing.entities.Listing;
import com.blinkstay.listing.enums.ListingStatus;
import com.blinkstay.listing.enums.SuspensionSource;
import com.blinkstay.listing.repository.ListingRepository;
import com.blinkstay.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingModerationService {
    private final ListingRepository listingRepo;

    private final RoomService roomService;

    private final CacheManager cacheManager;

    /**
     * Host blocked: every non-suspended listing becomes SUSPENDED (OWNER_BLOCKED).
     */
    @Transactional
    public int suspendAllByManager(UUID managerId, String reason) {
        return suspendAll(managerId, reason, SuspensionSource.OWNER_BLOCKED);
    }

    /**
     * Host deleted their account: listings go offline but are KEPT for 30 days (OWNER_DELETED).
     */
    @Transactional
    public int suspendAllForDeletedOwner(UUID managerId) {
        return suspendAll(managerId, "The owner deleted their account", SuspensionSource.OWNER_DELETED);
    }

    /**
     * Host unblocked: only listings suspended BY THE BLOCK are restored.
     */
    @Transactional
    public int restoreAfterUnblock(UUID managerId) {
        return restoreAll(managerId, SuspensionSource.OWNER_BLOCKED);
    }

    /**
     * Deleted host restored by an admin: only listings suspended BY THE DELETE are restored.
     */
    @Transactional
    public int restoreAfterOwnerRestored(UUID managerId) {
        return restoreAll(managerId, SuspensionSource.OWNER_DELETED);
    }

    // listings an admin had already suspended are left alone (they keep source ADMIN)
    private int suspendAll(UUID managerId, String reason, SuspensionSource source) {
        List<Listing> listings = listingRepo.findByManagerIdAndStatusNot(managerId, ListingStatus.SUSPENDED);
        LocalDateTime now = LocalDateTime.now();

        for (Listing l : listings) {
            l.setStatusBeforeSuspension(l.getStatus());
            l.setStatus(ListingStatus.SUSPENDED);
            l.setSuspensionSource(source);
            l.setSuspensionReason(reason);
            l.setSuspendedAt(now);
        }
        listingRepo.saveAll(listings);
        evictListingCaches();

        log.info("Suspended {} listings of manager {} ({})", listings.size(), managerId, source);
        return listings.size();
    }

    private int restoreAll(UUID managerId, SuspensionSource source) {
        List<Listing> listings = listingRepo.findByManagerIdAndStatusAndSuspensionSource(managerId,
                ListingStatus.SUSPENDED, source);

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

        log.info("Restored {} listings of manager {} ({})", listings.size(), managerId, source);
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