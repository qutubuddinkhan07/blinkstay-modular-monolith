package blinkstay.auth.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import blinkstay.auth.dto.AddUserDto;
import blinkstay.auth.dto.ApiResponse;
import blinkstay.auth.dto.EmailOtpVerifyDto;
import blinkstay.auth.dto.UserResponseDto;
import blinkstay.auth.entities.User;
import blinkstay.auth.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v2/user")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {
	private final UserService userService;

	@GetMapping("/{userId}")
	public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable("userId") Long userId) {
		User user = userService.getUserById(userId);
		ApiResponse<User> apiResponse = ApiResponse.<User>builder().success(true).message("Response").data(user)
				.build();

		return ResponseEntity.ok(apiResponse);
	}

	@GetMapping
	public ResponseEntity<ApiResponse<User>> getUserByEmail(@RequestParam String userEmail) {
		User user = userService.getUserByEmail(userEmail);
		ApiResponse<User> apiResponse = ApiResponse.<User>builder().success(true).message("Response").data(user)
				.build();

		return ResponseEntity.ok(apiResponse);
	}

	@DeleteMapping("/{userId}")
	public ResponseEntity<ApiResponse<String>> deleteUserByEmail(Long userId) {
		String serviceResponse = userService.deleteUserByEmail(userId);
		ApiResponse<String> apiResponse = ApiResponse.<String>builder().success(false).message("User deletion")
				.data(serviceResponse).build();

		return ResponseEntity.ok(apiResponse);
	}

	@PostMapping(value = "/register-init", consumes = "multipart/form-data")
	public ResponseEntity<ApiResponse<String>> initiateRegistration(
			@Valid @RequestPart("userData") @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AddUserDto.class))) AddUserDto dto,
			@RequestPart(value = "profileImg", required = true) MultipartFile profileImg) {
		try {
			String serviceResponse = userService.initiateUserRegistration(dto, profileImg);

			return ResponseEntity.ok(ApiResponse.<String>builder().success(true).message("User registration")
					.data(serviceResponse).build());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<ApiResponse<UserResponseDto>> verifyOtp(@RequestBody EmailOtpVerifyDto dto) {
		UserResponseDto userResponseDto = userService.verifyOtpAndRegister(dto.getEmail(), dto.getOtp());
		ApiResponse<UserResponseDto> apiResponse = ApiResponse.<UserResponseDto>builder().success(true)
				.message("User registered").data(userResponseDto).build();
		return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
	}

	@GetMapping("/allusers")
	public ResponseEntity<ApiResponse<List<User>>> getAllUsersController() {
		List<User> serviceReponse = userService.getAllUsers();
		ApiResponse<List<User>> apiResponse = ApiResponse.<List<User>>builder().success(true).message("All users")
				.data(serviceReponse).build();
		return ResponseEntity.ok(apiResponse);
	}

	// UPDATE profile image (delete old, upload new)
	@PutMapping("/{userId}/image")
	public ResponseEntity<ApiResponse<User>> updateUserImageController(@PathVariable("userId") Long userId,
			@RequestParam("image") MultipartFile image) {
		User updatedUser = userService.updateProfileImage(userId, image);
		ApiResponse<User> apiResponse = ApiResponse.<User>builder().success(true).message("All users").data(updatedUser)
				.build();
		return ResponseEntity.ok(apiResponse);
	}

	// REPLACE profile image (keep same URL)
	@PutMapping("/{userId}/image/replace")
	public ResponseEntity<ApiResponse<User>> replaceProfileImage(@PathVariable Long userId,
			@RequestParam("image") MultipartFile image) {
		User updatedUser = userService.replaceProfileImage(userId, image);
		ApiResponse<User> apiResponse = ApiResponse.<User>builder().success(true).message("All users").data(updatedUser)
				.build();
		return ResponseEntity.ok(apiResponse);
	}

	// EFFICIENT UPDATE using overwrite
	@PutMapping("/{userId}/image/update")
	public ResponseEntity<ApiResponse<User>> updateProfileImageEfficient(@PathVariable Long userId,
			@RequestParam("image") MultipartFile image) {
		User updatedUser = userService.updateProfileImageEfficient(userId, image);
		ApiResponse<User> apiResponse = ApiResponse.<User>builder().success(true).message("All users").data(updatedUser)
				.build();
		return ResponseEntity.ok(apiResponse);
	}

	@DeleteMapping("/{userId}/image")
	public ResponseEntity<ApiResponse<User>> deleteProfileImage(@PathVariable("userId") Long userId) {
		User user = userService.deleteProfileImage(userId);
		ApiResponse<User> apiResponse = ApiResponse.<User>builder().success(true)
				.message("Profile image deleted successfully").data(user).build();
		return ResponseEntity.ok(apiResponse);
	}

	// BULK DELETE user images
	@DeleteMapping("/images/bulk")
	public ResponseEntity<ApiResponse<Integer>> deleteMultipleImages(List<Long> userIds) {
		userService.deleteMultipleUserImage(userIds);
		ApiResponse<Integer> apiResponse = ApiResponse.<Integer>builder().success(true)
				.message("Images deleted successfully in bulk").data(userIds.size()).build();
		return ResponseEntity.ok(apiResponse);
	}

	// Get image details (for debugging)
	@GetMapping("/{userId}/image-details")
	public ResponseEntity<ApiResponse<?>> getImageDetails(@PathVariable("userId") Long userId) {
		Map<String, Object> serviceResponse = userService.getImageDetails(userId);
		ApiResponse<Map<String, Object>> apiResponse = ApiResponse.<Map<String, Object>>builder().success(true)
				.message("Image details").data(serviceResponse).build();
		return ResponseEntity.ok(apiResponse);
	}

}
