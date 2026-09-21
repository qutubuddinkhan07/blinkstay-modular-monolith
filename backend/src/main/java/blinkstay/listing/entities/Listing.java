package blinkstay.listing.entities;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import blinkstay.listing.enums.ListingCategory;
import blinkstay.listing.enums.ListingStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Listing {
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME) // Generates time order sequential UUIDs
	@JdbcTypeCode(SqlTypes.BINARY) // Maps java.util.UUID directly to MySQL BINARY(16)
	@Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
	private UUID id;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String location;

	@Column(nullable = false, length = 2000)
	private String description;

	@Column(nullable = false)
	private String country;

	@ElementCollection
	@CollectionTable(name = "listing_amenities", joinColumns = @JoinColumn(name = "listing_id"))
	@Column(name = "amenity")
	private List<String> amenities;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ListingStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ListingCategory category;

	// ID of the Hotel Manager from the User/Auth module
	@Column(nullable = false)
	private UUID managerId;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;
}
