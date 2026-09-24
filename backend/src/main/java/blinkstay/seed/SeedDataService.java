package blinkstay.seed;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import blinkstay.listing.entities.Listing;
import blinkstay.listing.entities.ListingGeometry;
import blinkstay.listing.entities.ListingImage;
import blinkstay.listing.enums.ListingStatus;
import blinkstay.listing.repository.ListingGeometryRepository;
import blinkstay.listing.repository.ListingImageRepository;
import blinkstay.listing.repository.ListingRepository;
import blinkstay.room.service.RoomService;
import blinkstay.seed.dto.SeedGeometry;
import blinkstay.seed.dto.SeedImage;
import blinkstay.seed.dto.SeedListing;
import blinkstay.seed.dto.SeedRoom;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class SeedDataService {

	@Value("${blinkstay.seed.enabled:false}")
	private boolean seedEnabled;

	private final ObjectMapper objectMapper;

	private final ListingRepository listingRepository;

	private final ListingImageRepository listingImageRepository;

	private final ListingGeometryRepository listingGeometryRepository;

	private final HotelManagerProvider hotelManagerProvider;

	private final SeedRandomizer seedRandomizer;

	private final SeedRoomGenerator seedRoomGenerator;

	private final RoomService roomService;

	@Transactional
	public void seed() {
		if (!seedEnabled) {
			return;
		}

		// Don't seed if data already exists
		if (listingRepository.count() > 0) {

			System.out.println("BlinkStay seed skipped: listings already exist.");

			return;
		}

		System.out.println("BlinkStay seed started...");

		List<SeedListing> seedListings = loadListings();

		List<UUID> managerIds = hotelManagerProvider.getHotelManagerIds();

		if (managerIds.isEmpty()) {

			throw new IllegalStateException("Cannot seed listings because no HOTEL_MANAGER " + "users were found.");
		}

		for (SeedListing seed : seedListings) {

			createListing(seed, managerIds);
		}

		System.out.println("BlinkStay seed completed. " + seedListings.size() + " listings inserted.");
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

	// create each listing
	private void createListing(SeedListing seed, List<UUID> managerIds) {

		UUID managerId = seedRandomizer.randomManager(managerIds);

		Listing listing = Listing.builder()

				.title(seed.getTitle())

				.description(seed.getDescription())

				.location(seed.getLocation())

				.country(seed.getCountry())

				.amenities(seed.getAmenities())

				.category(SeedEnumMapper.toCategory(seed.getCategory()))

				.status(ListingStatus.PUBLISHED)

				.managerId(managerId)

				.build();

		listing = listingRepository.save(listing);

		saveImages(listing.getId(), seed.getImage());

		saveGeometry(listing.getId(), seed.getGeometry());

		createRooms(listing.getId(), seed);
	}

	private void saveImages(UUID listingId, List<SeedImage> images) {

		if (images == null || images.isEmpty()) {
			return;
		}

		List<ListingImage> entities = new ArrayList<>();

		for (int i = 0; i < images.size(); i++) {

			SeedImage image = images.get(i);

			ListingImage entity = ListingImage.builder()

					.listingId(listingId)

					.imageUrl(image.getUrl())

					.publicId(image.getFilename())

					.displayOrder(i)

					.build();

			entities.add(entity);
		}

		listingImageRepository.saveAll(entities);
	}

	private void saveGeometry(UUID listingId, SeedGeometry geometry) {

		if (geometry == null || geometry.getCoordinates() == null || geometry.getCoordinates().length < 2) {

			return;
		}

		BigDecimal longitude = geometry.getCoordinates()[0];

		BigDecimal latitude = geometry.getCoordinates()[1];

		ListingGeometry entity = ListingGeometry.builder()

				.listingId(listingId)

				.address(
						// You can improve this later
						// with Mapbox reverse geocoding
						"Seed location")

				.latitude(latitude)

				.longitude(longitude)

				.build();

		listingGeometryRepository.save(entity);
	}

	// create rooms
	private void createRooms(UUID listingId, SeedListing seed) {

		List<SeedRoom> rooms = seed.getRooms();

		if (rooms == null || rooms.isEmpty()) {

			rooms = seedRoomGenerator.generateRooms(seed.getPrice());
		}

		roomService.createSeedRooms(listingId, rooms, seed.getPrice());
	}
}
