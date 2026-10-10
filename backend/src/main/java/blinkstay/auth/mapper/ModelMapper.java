package blinkstay.auth.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import blinkstay.auth.dtos.AdminUserDto;
import blinkstay.auth.dtos.UserResponseDto;
import blinkstay.auth.entities.User;

@Component("authModelMapper")
public class ModelMapper {
	public UserResponseDto userToUserResponseDto(User user) {
		UserResponseDto userResponseDto = UserResponseDto.builder().id(user.getId().toString())
				.username(user.getUsername()).email(user.getEmail()).phone(user.getPhone()).bio(user.getBio())
				.roles(user.getRoles()).isActive(user.getIsActive()).profileImgUrl(user.getProfileImgUrl())
				.createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();

		return userResponseDto;
	}

	public List<UserResponseDto> usersToResponseDtos(List<User> users) {
		List<UserResponseDto> userDtos = users.stream().map(user -> userToUserResponseDto(user)).toList();

		return userDtos;
	}

	public AdminUserDto userToAdminUserDto(User user) {
		return AdminUserDto.builder().id(user.getId().toString()).username(user.getUsername()).email(user.getEmail())
				.roles(user.getRoles()).blocked(!Boolean.TRUE.equals(user.getIsActive()))
				.blockReason(user.getBlockReason()).blockedAt(user.getBlockedAt())
				.profileImgUrl(user.getProfileImgUrl()).createdAt(user.getCreatedAt()).build();
	}
}
