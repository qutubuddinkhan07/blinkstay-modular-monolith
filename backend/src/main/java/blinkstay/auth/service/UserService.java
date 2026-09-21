package blinkstay.auth.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import blinkstay.auth.dtos.AddUserDto;
import blinkstay.auth.dtos.UserResponseDto;
import blinkstay.auth.entities.User;

public interface UserService {
	User helperGetUserId(UUID userId);

	UserResponseDto getUserById(UUID userId);

	UserResponseDto getUserByEmail(String userEmail);

	String deleteUserByEmail(UUID userId);

	List<UserResponseDto> getAllUsers();

	Map<String, Object> getImageDetails(UUID userId);

	String initiateUserRegistration(AddUserDto addUserDto, MultipartFile profileImg) throws IOException;

	UserResponseDto verifyOtpAndRegister(String email, String otp);

	UserResponseDto updateProfileImage(UUID userId, MultipartFile image);

	UserResponseDto replaceProfileImage(UUID userId, MultipartFile newImage);

	UserResponseDto updateProfileImageEfficient(UUID userId, MultipartFile newImage);

	UserResponseDto deleteProfileImage(UUID userId);

	void deleteMultipleUserImage(List<UUID> userIds);

	// To make a user Hotel_Manager
//	String userToHotelManager(Long userId, String email);
}