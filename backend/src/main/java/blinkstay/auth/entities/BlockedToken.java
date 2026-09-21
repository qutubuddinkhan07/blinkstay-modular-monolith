package blinkstay.auth.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedToken {
	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.TIME) // Generates time order sequential UUIDs
	@JdbcTypeCode(SqlTypes.BINARY) // Maps java.util.UUID directly to MySQL BINARY(16)
	@Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "token", nullable = false, unique = true, length = 1000)
	private String token;

	private LocalDateTime blockedAt;
	private LocalDateTime expiresAt;
}
