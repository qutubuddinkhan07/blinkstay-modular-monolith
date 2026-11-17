package blinkstay.auth.mapper;

import blinkstay.auth.dtos.*;
import blinkstay.auth.entities.HotelManagerProfile;
import blinkstay.auth.entities.HotelManagerRequest;
import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;
import blinkstay.auth.scheduler.DeletedAccountPurgeJob;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component("authModelMapper")
public class ModelMapper {
    public UserResponseDto userToUserResponseDto(User user) {
        UserResponseDto userResponseDto = UserResponseDto.builder().id(user.getId().toString())
                .username(user.getUsername()).email(user.getEmail()).phone(user.getPhone()).bio(user.getBio())
                .roles(user.getRoles()).isActive(user.getIsActive()).profileImgUrl(user.getProfileImgUrl())
                .createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();

        return userResponseDto;
    }

    public List<UserResponseDto> userToResponseDtos(List<User> users) {
        List<UserResponseDto> userDtos = users.stream().map(user -> userToUserResponseDto(user)).toList();

        return userDtos;
    }

    private static final List<UserRole> PRIORITY = List.of(UserRole.ADMIN, UserRole.HOTEL_MANAGER, UserRole.USER);

    private List<UserRole> displayRoles(Set<UserRole> roles) {
        List<UserRole> ordered = PRIORITY.stream().filter(roles::contains).toList();
        return ordered.size() > 1 ? ordered.stream().filter(r -> r != UserRole.USER).toList() : ordered;
    }

    public AdminUserDto userToAdminUserDto(User user) {
        List<UserRole> roles = displayRoles(user.getRoles());
        boolean deleted = user.getDeletedAt() != null;
        // a deleted account has isActive = false too, so "blocked" must come from blockedAt
        boolean blocked = deleted ? user.getBlockedAt() != null : !Boolean.TRUE.equals(user.getIsActive());

        return AdminUserDto.builder().id(user.getId().toString()).username(user.getUsername()).email(user.getEmail())
                .roles(roles).primaryRole(roles.isEmpty() ? null : roles.get(0)).blocked(blocked)
                .blockReason(user.getBlockReason()).blockedAt(user.getBlockedAt())
                .profileImgUrl(user.getProfileImgUrl()).createdAt(user.getCreatedAt()).deleted(deleted)
                .deletedAt(user.getDeletedAt())
                .purgeDueAt(deleted ? user.getDeletedAt().plusDays(DeletedAccountPurgeJob.RETENTION_DAYS) : null)
                .dataPurged(user.getPurgedAt() != null).build();
    }

    public HostRequestResponseDto hostRequestToDto(HotelManagerRequest r) {
        return HostRequestResponseDto.builder().id(r.getId()).status(r.getStatus()).businessName(r.getBusinessName())
                .address(r.getAddress()).description(r.getDescription()).website(r.getWebsite())
                .rejectionReason(r.getRejectionReason()).reviewedAt(r.getReviewedAt()).createdAt(r.getCreatedAt())
                .build();
    }

    public AdminHostRequestDto hostRequestToAdminDto(HotelManagerRequest r, User applicant) {
        return AdminHostRequestDto.builder().id(r.getId()).userId(r.getUserId())
                .username(applicant == null ? null : applicant.getUsername())
                .email(applicant == null ? null : applicant.getEmail())
                .profileImgUrl(applicant == null ? null : applicant.getProfileImgUrl()).status(r.getStatus())
                .businessName(r.getBusinessName()).address(r.getAddress()).description(r.getDescription())
                .website(r.getWebsite()).rejectionReason(r.getRejectionReason()).reviewedAt(r.getReviewedAt())
                .createdAt(r.getCreatedAt()).build();
    }

    public HostProfileDto hostProfileToDto(HotelManagerProfile p) {
        return HostProfileDto.builder().businessName(p.getBusinessName()).address(p.getAddress())
                .description(p.getDescription()).website(p.getWebsite()).approvedAt(p.getApprovedAt())
                .updatedAt(p.getUpdatedAt()).build();
    }
}
