package blinkstay.seed.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeedListing {

	private String title;

	private String description;

	private BigDecimal price;

	private String location;

	private String country;

	private List<SeedImage> image;

	private List<String> amenities;

	private SeedOwner owner;

	private String status;

	private List<SeedRoom> rooms;

	private SeedGeometry geometry;

	private String category;
}