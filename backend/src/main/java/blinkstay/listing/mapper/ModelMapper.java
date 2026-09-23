package blinkstay.listing.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import blinkstay.listing.dto.AddListingDto;
import blinkstay.listing.dto.GeometryDto;
import blinkstay.listing.dto.ImageDto;
import blinkstay.listing.dto.ImageUploadResult;
import blinkstay.listing.dto.ListingDetailsResponseDto;
import blinkstay.listing.entities.Listing;
import blinkstay.listing.entities.ListingGeometry;
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

	public GeometryDto geometryToGeometryDto(ListingGeometry geometry) {
		return GeometryDto.builder().address(geometry.getAddress()).latitude(geometry.getLatitude())
				.longitude(geometry.getLongitude()).build();
	}

	private List<ImageDto> listingImagesToImageDto(List<ListingImage> images) {
		return images.stream().map(
				image -> ImageDto.builder().imageUrl(image.getImageUrl()).displayOrder(image.getDisplayOrder()).build())
				.toList();
	}

	public ImageDto listingImageToImageDto(ListingImage listingImage) {
		return ImageDto.builder().id(listingImage.getId()).imageUrl(listingImage.getImageUrl())
				.publicId(listingImage.getPublicId()).displayOrder(listingImage.getDisplayOrder()).build();
	}

	public ListingDetailsResponseDto listingDetailsMapper(Listing listing, ListingGeometry geometry,
			List<ListingImage> images) {
		return ListingDetailsResponseDto.builder().id(listing.getId()).title(listing.getTitle())
				.location(listing.getLocation()).description(listing.getDescription()).country(listing.getCountry())
				.amenities(listing.getAmenities()).status(listing.getStatus()).category(listing.getCategory())
				.geometry(geometry == null ? null : geometryToGeometryDto(geometry))
				.images(listingImagesToImageDto(images)).createdAt(listing.getCreatedAt())
				.updatedAt(listing.getUpdatedAt()).build();
	}
}
