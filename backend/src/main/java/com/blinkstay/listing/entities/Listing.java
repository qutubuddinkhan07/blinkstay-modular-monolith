package com.blinkstay.listing.entities;

import com.blinkstay.listing.enums.ListingCategory;
import com.blinkstay.listing.enums.ListingStatus;
import com.blinkstay.listing.enums.SuspensionSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "listings", indexes = {@Index(name = "idx_listings_managerId", columnList = "managerId"),
        @Index(name = "idx_listings_status_country", columnList = "status,country")})
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
    @Column(name = "id", updatable = false, nullable = false)
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
    @Builder.Default
    private ListingStatus status = ListingStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // a new enum value never needs a column change
    private SuspensionSource suspensionSource;

    @Enumerated(EnumType.STRING)
    private ListingStatus statusBeforeSuspension;

    @Column(length = 500)
    private String suspensionReason;

    private LocalDateTime suspendedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingCategory category;

    // ID of the Hotel Manager from the User/Auth module
    @Column(nullable = false)
    private UUID managerId;

    // updated by the review module (through ListingRatings), never edited by hand
    @Column(precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int reviewCount = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
