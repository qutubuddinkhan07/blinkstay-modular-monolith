package com.blinkstay.listing.serviceImpl;

import com.blinkstay.common.stats.ListingStats;
import com.blinkstay.listing.enums.ListingStatus;
import com.blinkstay.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ListingStatsImpl implements ListingStats {
    private final ListingRepository listingRepo;

    @Override
    public Map<String, Long> countByStatus() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (ListingStatus s : ListingStatus.values()) {
            result.put(s.name(), listingRepo.countByStatus(s));
        }

        return result;
    }
}
