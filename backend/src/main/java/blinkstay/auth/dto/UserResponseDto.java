package blinkstay.auth.dto;

import blinkstay.auth.enums.UserRole;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private Long id;
    private String username;
    private String email;
    private UserRole role;
    private String password;
    private Boolean isACtive;
    private String profileImgUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
