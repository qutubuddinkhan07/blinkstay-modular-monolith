package blinkstay.listing.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import blinkstay.listing.constants.ImagePublicIds;
import blinkstay.listing.entities.Listing;
import blinkstay.listing.entities.ListingGeometry;
import blinkstay.listing.entities.ListingImage;
import blinkstay.listing.enums.ListingStatus;
import blinkstay.listing.repository.ListingGeometryRepository;
import blinkstay.listing.repository.ListingImageRepository;
import blinkstay.listing.repository.ListingRepository;
import blinkstay.listing.service.ListingImportService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListingImportServiceImpl implements ListingImportService {
	private final ListingRepository listingRepo;
	private final ListingImageRepository imageRepo;
	private final ListingGeometryRepository geometryRepo;

	@Override
	@Transactional(readOnly = true)
	public boolean hasListings() {
		return listingRepo.count() > 0;
	}

	@Override
	@Transactional
	public UUID importListing(ListingImportCommand c) {
		Listing listing = listingRepo.save(Listing.builder().title(c.title()).description(c.description())
				.location(c.location()).country(c.country()).amenities(c.amenities()).category(c.category())
				.status(ListingStatus.PUBLISHED).managerId(c.managerId()).build());
		UUID id = listing.getId();

		List<String> urls = c.imageUrls() == null ? List.of() : c.imageUrls();
		List<ListingImage> images = new ArrayList<>();
		for (int i = 0; i < urls.size(); i++) {
			images.add(ListingImage.builder().listingId(id).imageUrl(urls.get(i))
					.publicId(ImagePublicIds.SEED_PREFIX + id + "_" + (i + 1)).displayOrder(i).build());
		}
		if (!images.isEmpty()) {
			imageRepo.saveAll(images);
		}

		if (c.latitude() != null && c.longitude() != null) {
			geometryRepo.save(ListingGeometry.builder().listingId(id).address(c.address()).latitude(c.latitude())
					.longitude(c.longitude()).build());
		}
		return id;
	}
}
