package blinkstay.auth.dtos;

import java.time.LocalDateTime;
import java.util.Set;

import blinkstay.auth.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserDto {
	private String id;
	private String username;
	private String email;
	private Set<UserRole> roles;
	private boolean blocked;
	private String blockReason;
	private LocalDateTime blockedAt;
	private String profileImgUrl;
	private LocalDateTime createdAt;
}
