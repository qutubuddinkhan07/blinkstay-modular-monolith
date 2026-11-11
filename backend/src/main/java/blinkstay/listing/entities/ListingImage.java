package blinkstay.listing.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "listing_images", indexes = {@Index(name = "idx_listing_images_listing_id", columnList = "listing_id")})
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
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID listingId;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    @Column(nullable = false)
    private String publicId;

    private Integer displayOrder;
}
