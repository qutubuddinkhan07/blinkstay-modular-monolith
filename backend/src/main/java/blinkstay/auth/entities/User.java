package blinkstay.auth.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import blinkstay.auth.enums.UserRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
	@GeneratedValue(strategy = GenerationType.UUID)
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
	private String password;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "role")
	@Builder.Default
	@NotEmpty(message = "User must have at least one role")
	private Set<UserRole> roles = new HashSet<>();

	@Builder.Default
	private Boolean isActive = true;

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
