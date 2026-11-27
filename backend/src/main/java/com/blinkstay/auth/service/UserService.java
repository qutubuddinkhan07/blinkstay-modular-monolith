package com.blinkstay.auth.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.blinkstay.auth.dtos.AddUserDto;
import com.blinkstay.auth.dtos.PageResponseDto;
import com.blinkstay.auth.dtos.UpdateProfileDto;
import com.blinkstay.auth.dtos.UserResponseDto;
import com.blinkstay.auth.entities.User;

public interface UserService {
	User helperGetUserId(UUID userId);

	UserResponseDto getUserById(UUID userId);

	UserResponseDto getUserByEmail(String userEmail);

	String deleteUserById(UUID userId);

	PageResponseDto<UserResponseDto> getAllUsers(Pageable pageable);

	Map<String, Object> getImageDetails(UUID userId);

	String initiateUserRegistration(AddUserDto addUserDto, MultipartFile profileImg) throws IOException;

	UserResponseDto verifyOtpAndRegister(String email, String otp);

	UserResponseDto updateProfileImage(UUID userId, MultipartFile image);

	UserResponseDto deleteProfileImage(UUID userId);

	void deleteMultipleUserImage(List<UUID> userIds);

	// To make a user Hotel_Manager
//	String userToHotelManager(Long userId, String email);

	List<UUID> getApprovedHotelManagerIds();

	UserResponseDto updateProfile(UUID userId, UpdateProfileDto dto);

	void changePassword(UUID userId, String currentPassword, String newPassword);
}