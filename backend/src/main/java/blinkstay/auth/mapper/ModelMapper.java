package blinkstay.auth.mapper;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import blinkstay.auth.dtos.AdminUserDto;
import blinkstay.auth.dtos.UserResponseDto;
import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;

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
		return AdminUserDto.builder().id(user.getId().toString()).username(user.getUsername()).email(user.getEmail())
				.roles(roles).primaryRole(roles.isEmpty() ? null : roles.get(0))
				.blocked(!Boolean.TRUE.equals(user.getIsActive())).blockReason(user.getBlockReason())
				.blockedAt(user.getBlockedAt()).profileImgUrl(user.getProfileImgUrl()).createdAt(user.getCreatedAt())
				.build();
	}

}
