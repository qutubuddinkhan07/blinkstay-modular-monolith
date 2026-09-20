package blinkstay.auth.dto;

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
	private Long id;
	private String username;
	private String email;
	private Set<UserRole> roles;
	private String password;
	private Boolean isACtive;
	private String profileImgUrl;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
