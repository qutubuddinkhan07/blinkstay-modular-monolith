package blinkstay.auth.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
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

import blinkstay.auth.dtos.AddUserDto;
import blinkstay.auth.dtos.ApiResponse;
import blinkstay.auth.dtos.EmailOtpVerifyDto;
import blinkstay.auth.dtos.UserResponseDto;
import blinkstay.auth.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v2/user")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
@Validated
public class UserController {
	private final UserService userService;

	// ==========================================
	// PUBLIC / REGISTRATION ENDPOINTS
	// ==========================================
	@PostMapping(value = "/register-init", consumes = "multipart/form-data")
	public ResponseEntity<ApiResponse<String>> initiateRegistration(
			@Valid @RequestPart("userData") @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AddUserDto.class))) AddUserDto dto,
			@NotNull(message = "Profile image is required") @RequestPart(value = "profileImg", required = true) MultipartFile profileImg) {
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

	// ==========================================
	// LOGGED-IN USER ENDPOINTS ("/me")
	// Safe: Uses JWT Authentication Principal
	// ==========================================

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserResponseDto>> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
		UUID currentUserId = UUID.fromString(userDetails.getUsername());
		UserResponseDto userDto = userService.getUserById(currentUserId);

		return ResponseEntity.ok(ApiResponse.<UserResponseDto>builder().success(true)
				.message("User profile fetched successfully").data(userDto).build());
	}

	// UPDATE profile image (delete old, upload new)
	@PutMapping(value = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<UserResponseDto>> updateMyProfileImage(
			@AuthenticationPrincipal UserDetails userDetails, @RequestParam("image") MultipartFile image) {
		UUID currentUserId = UUID.fromString(userDetails.getUsername());
		UserResponseDto updatedUser = userService.updateProfileImage(currentUserId, image);
		ApiResponse<UserResponseDto> apiResponse = ApiResponse.<UserResponseDto>builder().success(true)
				.message("All users").data(updatedUser).build();
		return ResponseEntity.ok(apiResponse);
	}

	@GetMapping("/allusers")
	public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllUsersController() {
		List<UserResponseDto> serviceReponse = userService.getAllUsers();
		ApiResponse<List<UserResponseDto>> apiResponse = ApiResponse.<List<UserResponseDto>>builder().success(true)
				.message("All users").data(serviceReponse).build();
		return ResponseEntity.ok(apiResponse);
	}

	@GetMapping("/{userId}")
	public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable("userId") UUID userId) {
		UserResponseDto userResponseDto = userService.getUserById(userId);
		ApiResponse<UserResponseDto> apiResponse = ApiResponse.<UserResponseDto>builder().success(true)
				.message("Response").data(userResponseDto).build();

		return ResponseEntity.ok(apiResponse);
	}

	@GetMapping
	public ResponseEntity<ApiResponse<UserResponseDto>> getUserByEmail(@RequestParam String userEmail) {
		UserResponseDto userResponseDto = userService.getUserByEmail(userEmail);
		ApiResponse<UserResponseDto> apiResponse = ApiResponse.<UserResponseDto>builder().success(true)
				.message("Response").data(userResponseDto).build();

		return ResponseEntity.ok(apiResponse);
	}

	@DeleteMapping("/{userId}")
	public ResponseEntity<ApiResponse<String>> deleteUserByEmail(UUID userId) {
		String serviceResponse = userService.deleteUserByEmail(userId);
		ApiResponse<String> apiResponse = ApiResponse.<String>builder().success(false).message("User deletion")
				.data(serviceResponse).build();

		return ResponseEntity.ok(apiResponse);
	}

	// REPLACE profile image (keep same URL)
	@PutMapping("/{userId}/image/replace")
	public ResponseEntity<ApiResponse<UserResponseDto>> replaceProfileImage(@PathVariable UUID userId,
			@RequestParam("image") MultipartFile image) {
		UserResponseDto updatedUser = userService.replaceProfileImage(userId, image);
		ApiResponse<UserResponseDto> apiResponse = ApiResponse.<UserResponseDto>builder().success(true)
				.message("Image replaced").data(updatedUser).build();
		return ResponseEntity.ok(apiResponse);
	}

	// EFFICIENT UPDATE using overwrite
	@PutMapping("/{userId}/image/update")
	public ResponseEntity<ApiResponse<UserResponseDto>> updateProfileImageEfficient(@PathVariable UUID userId,
			@RequestParam("image") MultipartFile image) {
		UserResponseDto updatedUser = userService.updateProfileImageEfficient(userId, image);
		ApiResponse<UserResponseDto> apiResponse = ApiResponse.<UserResponseDto>builder().success(true)
				.message("All users").data(updatedUser).build();
		return ResponseEntity.ok(apiResponse);
	}

	@DeleteMapping("/{userId}/image")
	public ResponseEntity<ApiResponse<UserResponseDto>> deleteProfileImage(@PathVariable("userId") UUID userId) {
		UserResponseDto userResponseDto = userService.deleteProfileImage(userId);
		ApiResponse<UserResponseDto> apiResponse = ApiResponse.<UserResponseDto>builder().success(true)
				.message("Profile image deleted successfully").data(userResponseDto).build();
		return ResponseEntity.ok(apiResponse);
	}

	// BULK DELETE user images
	@DeleteMapping("/images/bulk")
	public ResponseEntity<ApiResponse<Integer>> deleteMultipleImages(List<UUID> userIds) {
		userService.deleteMultipleUserImage(userIds);
		ApiResponse<Integer> apiResponse = ApiResponse.<Integer>builder().success(true)
				.message("Images deleted successfully in bulk").data(userIds.size()).build();
		return ResponseEntity.ok(apiResponse);
	}

	// Get image details (for debugging)
	@GetMapping("/{userId}/image-details")
	public ResponseEntity<ApiResponse<?>> getImageDetails(@PathVariable("userId") UUID userId) {
		Map<String, Object> serviceResponse = userService.getImageDetails(userId);
		ApiResponse<Map<String, Object>> apiResponse = ApiResponse.<Map<String, Object>>builder().success(true)
				.message("Image details").data(serviceResponse).build();
		return ResponseEntity.ok(apiResponse);
	}

}
