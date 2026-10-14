package blinkstay.auth.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import blinkstay.auth.enums.RequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "hotel_manager_requests", indexes = { @Index(name = "idx_hmr_user", columnList = "userId"),
		@Index(name = "idx_hmr_created", columnList = "status, createdAt") })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class HotelManagerRequest {
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME)
	@JdbcTypeCode(SqlTypes.BINARY)
	@Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
	private UUID id;

	@Column(nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false)
	private RequestStatus status;

	// Set to the user's id while the request is PENDING, null afterwards. The
	// unique constraint then guarantees one pending request per user, even if two
	// requests arrive at the same moment (MySQL allows many NULLs in a unique
	// column).
	@Column(unique = true)
	private UUID pendingUserId;

	@Column(nullable = false, length = 120)
	private String businessName;

	@Column(nullable = false, length = 255)
	private String address;

	@Column(nullable = false, length = 1000)
	private String description;

	@Column(length = 255)
	private String website;

	private UUID reviewedBy;

	private LocalDateTime reviewedAt;

	@Column(length = 500)
	private String rejectionReason;

	// two admins approving the same request: the second one fails with a 409
	@Version
	private Long version;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;
}
