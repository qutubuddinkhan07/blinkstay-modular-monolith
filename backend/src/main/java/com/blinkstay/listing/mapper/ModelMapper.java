package com.blinkstay.listing.mapper;

import com.blinkstay.common.image.ImageUploadResult;
import com.blinkstay.listing.dto.AddListingDto;
import com.blinkstay.listing.dto.GeometryDto;
import com.blinkstay.listing.dto.ImageDto;
import com.blinkstay.listing.dto.ListingDetailsResponseDto;
import com.blinkstay.listing.entities.Listing;
import com.blinkstay.listing.entities.ListingGeometry;
import com.blinkstay.listing.entities.ListingImage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component("listingModelMapper")
public class ModelMapper {

    public Listing addListingDtoToListing(AddListingDto dto, UUID managerId) {
        return Listing.builder().title(dto.getTitle()).location(dto.getLocation())
                .description(dto.getDescription()).country(dto.getCountry()).amenities(dto.getAmenities())
                .category(dto.getCategory()).managerId(managerId).build();
    }

    // ImageUploadResult is a record: url() and publicId(), no getters, no builder
    public ListingImage imageUploadResultToListingImage(ImageUploadResult imageUploadResult, UUID listingId,
                                                        Integer order) {
        return ListingImage.builder().listingId(listingId).imageUrl(imageUploadResult.url())
                .publicId(imageUploadResult.publicId()).displayOrder(order).build();
    }

    public GeometryDto geometryToGeometryDto(ListingGeometry geometry) {
        return GeometryDto.builder().address(geometry.getAddress()).latitude(geometry.getLatitude())
                .longitude(geometry.getLongitude()).build();
    }

    private List<ImageDto> listingImagesToImageDto(List<ListingImage> images) {
        return images.stream().map(image -> ImageDto.builder().id(image.getId()).imageUrl(image.getImageUrl())
                .displayOrder(image.getDisplayOrder()).build()).toList();
    }

    public ImageDto listingImageToImageDto(ListingImage listingImage) {
        return ImageDto.builder().id(listingImage.getId()).imageUrl(listingImage.getImageUrl())
                .publicId(listingImage.getPublicId()).displayOrder(listingImage.getDisplayOrder()).build();
    }

    public ListingDetailsResponseDto listingDetailsMapper(Listing listing, ListingGeometry geometry,
                                                          List<ListingImage> images) {
        return ListingDetailsResponseDto.builder().id(listing.getId()).title(listing.getTitle())
                .location(listing.getLocation()).description(listing.getDescription()).country(listing.getCountry())
                .amenities(listing.getAmenities()).status(listing.getStatus())
                .suspensionSource(listing.getSuspensionSource()).suspensionReason(listing.getSuspensionReason())
                .suspendedAt(listing.getSuspendedAt()).category(listing.getCategory())
                .geometry(geometry == null ? null : geometryToGeometryDto(geometry))
                .images(listingImagesToImageDto(images)).createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt()).build();
    }
}
