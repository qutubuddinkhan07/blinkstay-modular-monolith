package blinkstay.listing.serviceImpl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import blinkstay.listing.dto.PublishedListingDto;
import blinkstay.listing.entities.Listing;
import blinkstay.listing.entities.ListingGeometry;
import blinkstay.listing.entities.ListingImage;
import blinkstay.listing.repository.ListingGeometryRepository;
import blinkstay.listing.repository.ListingImageRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PublishedListingAssembler {
	private final ListingImageRepository listingImageRepo;
	private final ListingGeometryRepository listingGeometryRepo;

	public List<PublishedListingDto> assemble(List<Listing> listings) {
		if (listings.isEmpty()) {
			return List.of();
		}

		List<UUID> ids = listings.stream().map(Listing::getId).toList();

		Map<UUID, List<String>> imagesByListing = listingImageRepo.findByListingIdInOrderByDisplayOrderAsc(ids).stream()
				.collect(Collectors.groupingBy(ListingImage::getListingId,
						Collectors.mapping(ListingImage::getImageUrl, Collectors.toList())));

		Map<UUID, ListingGeometry> geometryMap = listingGeometryRepo.findByListingIdIn(ids).stream()
				.collect(Collectors.toMap(ListingGeometry::getListingId, g -> g, (a, b) -> a));

		return listings.stream().map(l -> {
			ListingGeometry g = geometryMap.get(l.getId());
			return PublishedListingDto.builder().id(l.getId()).title(l.getTitle()).location(l.getLocation())
					.country(l.getCountry()).category(l.getCategory()).amenities(l.getAmenities())
					.imageUrls(imagesByListing.getOrDefault(l.getId(), List.of()))
					.address(g != null ? g.getAddress() : null).latitude(g != null ? g.getLatitude() : null)
					.longitude(g != null ? g.getLongitude() : null).build();
		}).toList();
	}
}
