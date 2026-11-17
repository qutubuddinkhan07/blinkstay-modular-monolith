package blinkstay.auth.entities;

import blinkstay.auth.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class User {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME) // Generates time order sequential UUIDs
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "username cannot be empty")
    @Column(nullable = false)
    private String username;

    @NotBlank(message = "email cannot be empty")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "password cannot be empty")
    @Column(nullable = false)
    @ToString.Exclude
    private String password;

    private Instant passwordChangedAt;

    @Column(length = 20)
    private String phone;

    @Column(length = 300)
    private String bio;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    @NotEmpty(message = "User must have at least one role")
    private Set<UserRole> roles = new HashSet<>();

    @Builder.Default
    private Boolean isActive = true;

    @Column(length = 500)
    private String blockReason;

    private LocalDateTime blockedAt; // Blocked: isActive = false and deletedAt = null.

    private LocalDateTime deletedAt; // Self-deleted: isActive = false and deletedAt set.

    private LocalDateTime purgedAt; // set by the 30-day purge job: image and reviews already removed

    // Stores Cloudinary image URL
    private String profileImgUrl;

    // Store Cluodinary public_id for deletion/updates
    private String imagePublicId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

	/*-
	* instead of @CreationTimeStamp can use this
	* but need to write write @Builder.Default for the builder
	* but @CreationTimeStamp handles both
	* @PrePersist
	  protected void onCreate(){
	      this.createdAt = LocalDateTime.now();
	  }
	 */
}
