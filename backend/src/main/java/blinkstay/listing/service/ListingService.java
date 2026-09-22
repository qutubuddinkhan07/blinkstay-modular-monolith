package blinkstay.listing.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import blinkstay.listing.dtos.AddListingDto;

public interface ListingService {
	String createListing(UUID managerId, AddListingDto addListingDto, List<MultipartFile> images);
}
