package blinkstay.listing.entities;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "listing_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ListingImage {
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME) // Generates time order sequential UUIDs
	@JdbcTypeCode(SqlTypes.BINARY) // Maps java.util.UUID directly to MySQL BINARY(16)
	@Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
	private UUID id;

	@Column(nullable = false)
	private UUID listingId;

	@Column(nullable = false, length = 1000)
	private String imageUrl;

	@Column(nullable = false)
	private String publicId;

	private Integer displayOrder;
}
