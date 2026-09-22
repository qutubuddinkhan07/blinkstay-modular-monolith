package blinkstay.listing.mapper;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import blinkstay.listing.dtos.AddListingDto;
import blinkstay.listing.dtos.ImageUploadResult;
import blinkstay.listing.entities.Listing;
import blinkstay.listing.entities.ListingImage;

@Component("listingModelMapper")
@SuppressWarnings({ "rawtypes" })
public class ModelMapper {
	public ImageUploadResult objectToImageUploadResult(Map uploadResult, Long size) {
		return ImageUploadResult.builder().url((String) uploadResult.get("secure_url"))
				.publicId((String) uploadResult.get("public_id")).size(size).format((String) uploadResult.get("format"))
				.build();
	}

	public Listing addListingDtoToListing(AddListingDto dto, UUID managerId) {
		Listing listing = Listing.builder().title(dto.getTitle()).location(dto.getLocation())
				.description(dto.getDescription()).country(dto.getCountry()).amenities(dto.getAmenities())
				.category(dto.getCategory()).managerId(managerId).build();

		return listing;
	}

	public ListingImage imageUploadResultToListingImage(ImageUploadResult imageUploadResult, UUID listingId,
			Integer order) {
		ListingImage listingImage = ListingImage.builder().listingId(listingId).imageUrl(imageUploadResult.getUrl())
				.publicId(imageUploadResult.getPublicId()).displayOrder(order).build();

		return listingImage;
	}

}
