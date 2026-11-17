package blinkstay.review.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(name = "uk_review_booking", columnNames = "bookingId"), indexes = {
        @Index(name = "idx_review_listing", columnList = "listingId,createdAt"),
        @Index(name = "idx_review_host", columnList = "hostId,createdAt"),
        @Index(name = "idx_review_guest", columnList = "guestId")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Review {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // plain UUIDs, no JPA relations: modules never share entities
    @Column(nullable = false)
    private UUID bookingId;
    @Column(nullable = false)
    private UUID listingId;
    @Column(nullable = false)
    private UUID guestId;
    @Column(nullable = false)
    private UUID hostId;

    // snapshots, so a review still reads right if the user or listing changes
    @Column(nullable = false)
    private String guestName;
    @Column(nullable = false)
    private String listingTitle;

    @Column(nullable = false)
    private int rating;

    @Column(length = 1000)
    private String comment;

    @Column(length = 1000)
    private String hostReply;
    private Instant hostRepliedAt;

    private Instant editedAt;

    // true while the guest's account is blocked: hidden reviews are ignored by lists and averages
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean hidden = false;

    @CreationTimestamp
    private Instant createdAt;
}