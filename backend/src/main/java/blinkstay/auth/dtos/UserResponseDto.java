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
public class UserResponseDto {
	private String id;
	private String username;
	private String email;
	private Set<UserRole> roles;
	private Boolean isActive;
	private String profileImgUrl;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
