package blinkstay.auth.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockedToken {
	@Id
	private String id;
	private LocalDateTime blockedAt;
	private LocalDateTime expiresAt;
}
