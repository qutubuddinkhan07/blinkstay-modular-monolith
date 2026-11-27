package com.blinkstay.listing.serviceImpl;

import com.blinkstay.common.seed.SeedListingRef;
import com.blinkstay.common.seed.SeedListingSource;
import com.blinkstay.listing.entities.Listing;
import com.blinkstay.listing.entities.ListingImage;
import com.blinkstay.listing.enums.ListingStatus;
import com.blinkstay.listing.repository.ListingImageRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeedListingSourceImpl implements SeedListingSource {

    @PersistenceContext
    private EntityManager em;

    private final ListingImageRepository listingImageRepo;

    @Override
    @Transactional(readOnly = true)
    public List<SeedListingRef> publishedListings() {
        List<Listing> listings = em.createQuery(
                        "select l from Listing l where l.status = :status order by l.id", Listing.class)
                .setParameter("status", ListingStatus.PUBLISHED)
                .getResultList();

        return listings.stream().map(l -> new SeedListingRef(l.getId(), l.getManagerId(), l.getTitle(),
                l.getLocation(),
                listingImageRepo.findByListingIdOrderByDisplayOrderAsc(l.getId()).stream().findFirst()
                        .map(ListingImage::getImageUrl).orElse(null))).toList();
    }
}