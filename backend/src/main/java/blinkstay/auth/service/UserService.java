package blinkstay.auth.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import blinkstay.auth.dto.AddUserDto;
import blinkstay.auth.dto.UserResponseDto;
import blinkstay.auth.entities.User;

public interface UserService {
	User getUserById(Long userId);

	User getUserByEmail(String userEmail);

	List<User> getAllUsers();

	Map<String, Object> getImageDetails(Long userId);

	String initiateUserRegistration(AddUserDto addUserDto, MultipartFile profileImg) throws IOException;

	UserResponseDto verifyOtpAndRegister(String email, String otp);

	User updateProfileImage(Long userId, MultipartFile image);

	User replaceProfileImage(Long userId, MultipartFile newImage);

	User updateProfileImageEfficient(Long userId, MultipartFile newImage);

	User deleteProfileImage(Long userId);

	void deleteMultipleUserImage(List<Long> userIds);

	// To make a user Hotel_Manager
//	String userToHotelManager(Long userId, String email);
}