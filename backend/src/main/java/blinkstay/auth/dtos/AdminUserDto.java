package blinkstay.auth.dtos;

import blinkstay.auth.enums.UserRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserDto {
    private String id;
    private String username;
    private String email;
    private List<UserRole> roles;
    private UserRole primaryRole;
    private boolean blocked;
    private String blockReason;
    private LocalDateTime blockedAt;
    private String profileImgUrl;
    private LocalDateTime createdAt;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime purgeDueAt; // when the image and reviews will be removed (null if not deleted)
    private boolean dataPurged;       // true once the 30-day purge has run
}
