package blinkstay.auth.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import blinkstay.auth.dto.UserResponseDto;
import blinkstay.auth.entities.User;

@Component
public class ModelMapper {
	public UserResponseDto userToUserResponseDto(User user) {
		UserResponseDto userResponseDto = UserResponseDto.builder().id(user.getId().toString())
				.username(user.getUsername()).email(user.getEmail()).roles(user.getRoles()).isActive(user.getIsActive())
				.profileImgUrl(user.getProfileImgUrl()).createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt())
				.build();

		return userResponseDto;
	}

	public List<UserResponseDto> usersToResponseDtos(List<User> users) {
		List<UserResponseDto> userDtos = users.stream().map(user -> userToUserResponseDto(user)).toList();

		return userDtos;
	}
}
