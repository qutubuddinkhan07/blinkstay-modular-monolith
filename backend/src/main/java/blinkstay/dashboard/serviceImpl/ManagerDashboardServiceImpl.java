package blinkstay.dashboard.serviceImpl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import blinkstay.dashboard.dto.ManagerDashboardDto;
import blinkstay.dashboard.service.ManagerDashboardService;
import blinkstay.listing.dto.ListingSummaryDto;
import blinkstay.listing.entities.Listing;
import blinkstay.listing.entities.ListingImage;
import blinkstay.listing.enums.ListingStatus;
import blinkstay.listing.repository.ListingImageRepository;
import blinkstay.listing.service.ListingService;
import blinkstay.room.dto.RoomSummaryDto;
import blinkstay.room.service.RoomService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManagerDashboardServiceImpl implements ManagerDashboardService {

	private final ListingService listingService;
	private final RoomService roomService;
	private final ListingImageRepository listingImageRepo;

	@Override
	public ManagerDashboardDto getManagerDashboard(UUID managerId) {

		List<Listing> listings = listingService.getListingsByManagerId(managerId);

		List<ListingSummaryDto> listingSummaries = listings.stream().map(listing -> {

			RoomSummaryDto roomSummary = roomService.getRoomSummary(listing.getId());

			String coverImage = listingImageRepo.findFirstByListingIdOrderByDisplayOrderAsc(listing.getId())
					.map(ListingImage::getImageUrl).orElse(null);

			return ListingSummaryDto.builder().id(listing.getId()).title(listing.getTitle())
					.location(listing.getLocation()).status(listing.getStatus()).category(listing.getCategory())
					.coverImage(coverImage).roomSummary(roomSummary).build();
		}).toList();

		int totalListings = listings.size();

		int publishedListings = (int) listings.stream().filter(l -> l.getStatus() == ListingStatus.PUBLISHED).count();

		int draftListings = (int) listings.stream().filter(l -> l.getStatus() == ListingStatus.DRAFT).count();

		int totalRooms = listingSummaries.stream().mapToInt(l -> l.getRoomSummary().getTotalRooms()).sum();

		int availableRooms = listingSummaries.stream().mapToInt(l -> l.getRoomSummary().getAvailableRooms()).sum();

		return ManagerDashboardDto.builder().totalListings(totalListings).publishedListings(publishedListings)
				.draftListings(draftListings).totalRooms(totalRooms).availableRooms(availableRooms)
				.listings(listingSummaries).build();
	}
}
