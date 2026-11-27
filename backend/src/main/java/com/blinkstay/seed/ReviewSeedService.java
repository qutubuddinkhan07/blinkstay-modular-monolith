package com.blinkstay.seed;

import com.blinkstay.common.seed.*;
import com.blinkstay.room.dto.RoomResponseDto;
import com.blinkstay.room.service.RoomService;
import com.blinkstay.seed.dto.SeedReviewData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Demo reviews: for each published listing, a few completed past stays by demo guests,
 * each with a review and sometimes a host reply. Idempotent and all-or-nothing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewSeedService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Value("${blinkstay.seed.reviews.enabled:false}")
    private boolean enabled;

    @Value("${blinkstay.seed.reviews.min-per-listing:3}")
    private int minPerListing;

    @Value("${blinkstay.seed.reviews.max-per-listing:6}")
    private int maxPerListing;

    private final ObjectMapper objectMapper;
    private final SeedListingSource listingSource;
    private final RoomService roomService;
    private final DemoGuestAccounts guestAccounts;
    private final PastStayWriter stayWriter;
    private final SeedReviewWriter reviewWriter;
    private final TransactionTemplate tx;

    public void seed() {
        if (!enabled) {
            return;
        }
        SeedReviewData data = load();
        tx.executeWithoutResult(status -> run(data));
    }

    private void run(SeedReviewData data) {
        List<DemoGuest> guests = guestAccounts.ensureGuests(data.getGuests().stream()
                .map(g -> new NewDemoGuest(g.getUsername(), g.getEmail())).toList());

        if (reviewWriter.existsForGuests(guests.stream().map(DemoGuest::id).toList())) {
            log.info("Review seed skipped: demo reviews already exist.");
            return;
        }

        List<SeedListingRef> listings = listingSource.publishedListings();
        if (listings.isEmpty()) {
            log.info("Review seed skipped: no published listings.");
            return;
        }

        LocalDate today = LocalDate.now(IST);
        Set<UUID> touched = new TreeSet<>();
        int reviews = 0;
        int replies = 0;

        for (SeedListingRef listing : listings) {
            List<RoomResponseDto> rooms = roomService.getRoomsByListingId(listing.listingId());
            if (rooms == null || rooms.isEmpty()) {
                continue;
            }

            // seeded by the listing id: the same database always gets the same reviews
            Random rnd = new Random(listing.listingId().getMostSignificantBits());
            int count = minPerListing + rnd.nextInt(maxPerListing - minPerListing + 1);
            List<DemoGuest> shuffled = new ArrayList<>(guests);
            Collections.shuffle(shuffled, rnd);

            for (int i = 0; i < count && i < shuffled.size(); i++) {
                DemoGuest guest = shuffled.get(i);
                RoomResponseDto room = rooms.get(rnd.nextInt(rooms.size()));

                int nights = 1 + rnd.nextInt(4);
                LocalDate checkOut = today.minusDays(7 + rnd.nextInt(170));
                LocalDate checkIn = checkOut.minusDays(nights);
                int maxGuests = room.getMaxGuests() == null ? 2 : room.getMaxGuests();
                int guestCount = Math.max(1, Math.min(maxGuests, 1 + rnd.nextInt(2)));
                Instant bookedAt = checkIn.minusDays(5 + rnd.nextInt(30)).atTime(11, 0).atZone(IST).toInstant();

                UUID bookingId = stayWriter.createCompletedStay(new PastStay(guest.id(), listing.hostId(),
                        listing.listingId(), room.getRoomId(), room.getRoomType().name(), room.getPrice(),
                        checkIn, checkOut, guestCount, listing.title(), listing.location(),
                        listing.coverImageUrl(), bookedAt));

                int rating = pickRating(rnd);
                Instant reviewedAt = checkOut.plusDays(1 + rnd.nextInt(5))
                        .atTime(9 + rnd.nextInt(12), rnd.nextInt(60)).atZone(IST).toInstant();

                String reply = null;
                Instant repliedAt = null;
                if (rnd.nextInt(3) == 0 && !data.getReplies().isEmpty()) {
                    reply = pick(data.getReplies(), rnd).replace("{guest}", firstName(guest.username()));
                    repliedAt = reviewedAt.plus(Duration.ofHours(24L + rnd.nextInt(48)));
                    replies++;
                }

                reviewWriter.add(new SeedReview(bookingId, listing.listingId(), guest.id(), listing.hostId(),
                        guest.username(), listing.title(), rating,
                        pick(data.getComments().get(String.valueOf(rating)), rnd),
                        reviewedAt, reply, repliedAt));
                reviews++;
            }
            touched.add(listing.listingId());
        }

        reviewWriter.refreshRatings(new ArrayList<>(touched));
        log.info("Review seed completed: {} reviews ({} with host replies) on {} listings.",
                reviews, replies, touched.size());
    }

    // about 60% five stars, 30% four, 10% three
    private static int pickRating(Random rnd) {
        int r = rnd.nextInt(10);
        return r < 6 ? 5 : r < 9 ? 4 : 3;
    }

    private static String pick(List<String> list, Random rnd) {
        return list.get(rnd.nextInt(list.size()));
    }

    private static String firstName(String username) {
        String trimmed = username.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }

    private SeedReviewData load() {
        try {
            return objectMapper.readValue(new ClassPathResource("seed/BlinkStay.reviews.json").getInputStream(),
                    SeedReviewData.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read BlinkStay.reviews.json", e);
        }
    }
}