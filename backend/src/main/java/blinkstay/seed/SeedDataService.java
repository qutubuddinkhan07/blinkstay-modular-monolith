package blinkstay.seed;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import blinkstay.listing.service.ListingImportService;
import blinkstay.listing.service.MapboxGeocodingService;
import blinkstay.listing.serviceImpl.ListingImportCommand;
import blinkstay.room.dto.NewRoom;
import blinkstay.room.enums.RoomCategory;
import blinkstay.room.service.RoomService;
import blinkstay.seed.dto.SeedGeometry;
import blinkstay.seed.dto.SeedImage;
import blinkstay.seed.dto.SeedListing;
import blinkstay.seed.dto.SeedRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeedDataService {

	@Value("${blinkstay.seed.enabled:false}")
	private boolean seedEnabled;

	@Value("${blinkstay.seed.reverse-geocode:false}")
	private boolean reverseGeocode;

	@Value("${blinkstay.seed.reverse-geocode-delay-ms:120}")
	private long reverseGeocodeDelayMs;

	private final ObjectMapper objectMapper;
	private final ListingImportService listingImportService;
	private final MapboxGeocodingService geocodingService;
	private final HotelManagerProvider hotelManagerProvider;
	private final SeedRandomizer seedRandomizer;
	private final SeedRoomGenerator seedRoomGenerator;
	private final RoomService roomService;
	private final TransactionTemplate tx;

	private record Prepared(SeedListing seed, UUID managerId, String address, List<NewRoom> rooms) {
	}

	// Deliberately NOT @Transactional: phase 1 calls the network, so no database
	// transaction may be open yet.
	public void seed() {
		if (!seedEnabled) {
			return;
		}
		if (listingImportService.hasListings()) {
			log.info("BlinkStay seed skipped: listings already exist.");
			return;
		}

		List<UUID> managerIds = hotelManagerProvider.getHotelManagerIds();
		if (managerIds.isEmpty()) {
			throw new IllegalStateException("Cannot seed listings because no HOTEL_MANAGER users were found.");
		}

		List<SeedListing> seedListings = loadListings();
		log.info("BlinkStay seed started: {} listings, reverse geocoding {}", seedListings.size(),
				reverseGeocode ? "on" : "off");

		// Phase 1: everything that needs the network, no transaction open
		List<Prepared> prepared = new ArrayList<>(seedListings.size());
		int resolved = 0;
		for (SeedListing seed : seedListings) {
			String address = null;
			if (reverseGeocode) {
				address = lookupAddress(seed.getGeometry());
				if (address != null) {
					resolved++;
				}
				pause();
			}
			if (address == null) {
				address = fallbackAddress(seed);
			}
			prepared.add(
					new Prepared(seed, seedRandomizer.randomManager(managerIds), cut(address, 255), toNewRooms(seed)));
		}
		if (reverseGeocode) {
			log.info("Reverse geocoding resolved {} of {} addresses; the rest use the listing's location text",
					resolved, seedListings.size());
		}

		// Phase 2: one transaction, all or nothing
		tx.executeWithoutResult(status -> prepared.forEach(this::insert));
		log.info("BlinkStay seed completed. {} listings inserted.", prepared.size());
	}

	private void insert(Prepared p) {
		SeedListing seed = p.seed();

		BigDecimal longitude = null;
		BigDecimal latitude = null;
		SeedGeometry g = seed.getGeometry();
		if (g != null && g.getCoordinates() != null && g.getCoordinates().length >= 2) {
			longitude = g.getCoordinates()[0];
			latitude = g.getCoordinates()[1];
		}

		List<String> imageUrls = seed.getImage() == null ? List.of()
				: seed.getImage().stream().map(SeedImage::getUrl).toList();

		UUID listingId = listingImportService
				.importListing(new ListingImportCommand(p.managerId(), seed.getTitle(), seed.getDescription(),
						seed.getLocation(), seed.getCountry(), SeedEnumMapper.toCategory(seed.getCategory()),
						seed.getAmenities(), imageUrls, latitude, longitude, p.address()));

		roomService.createRooms(listingId, p.rooms());
	}

	private List<NewRoom> toNewRooms(SeedListing seed) {
		List<SeedRoom> rooms = seed.getRooms();
		if (rooms == null || rooms.isEmpty()) {
			rooms = seedRoomGenerator.generateRooms(seed.getPrice());
		}
		return rooms.stream().map(r -> {
			BigDecimal adjustment = r.getPriceAdjustment() == null ? BigDecimal.ZERO : r.getPriceAdjustment();
			RoomCategory type = SeedRoomMapper.toRoomCategory(r.getType());
			return new NewRoom(type, seed.getPrice().add(adjustment), r.getTotalRooms(), maxGuestsFor(type));
		}).toList();
	}

	// guests PER ROOM, so seeded stays behave like real ones in the availability
	// check
	private static int maxGuestsFor(RoomCategory type) {
		return switch (type) {
		case SINGLE -> 1;
		case DOUBLE, TWIN, STUDIO -> 2;
		case DELUXE -> 3;
		case SUITE -> 4;
		case FAMILY -> 5;
		};
	}

	private String lookupAddress(SeedGeometry g) {
		if (g == null || g.getCoordinates() == null || g.getCoordinates().length < 2) {
			return null;
		}
		return geocodingService.reverseGeocode(g.getCoordinates()[1], g.getCoordinates()[0]); // [lon, lat]
	}

	private String fallbackAddress(SeedListing seed) {
		if (StringUtils.hasText(seed.getLocation())) {
			return seed.getLocation().trim();
		}
		if (StringUtils.hasText(seed.getCountry())) {
			return seed.getCountry().trim();
		}
		return "Address not available";
	}

	private static String cut(String s, int max) {
		return s.length() <= max ? s : s.substring(0, max);
	}

	private void pause() {
		try {
			Thread.sleep(reverseGeocodeDelayMs);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private List<SeedListing> loadListings() {
		try {
			ClassPathResource resource = new ClassPathResource("seed/BlinkStay.listings.json");
			return objectMapper.readValue(resource.getInputStream(), new TypeReference<List<SeedListing>>() {
			});
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read BlinkStay.listings.json", e);
		}
	}
}