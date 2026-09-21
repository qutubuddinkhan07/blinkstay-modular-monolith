package blinkstay.auth.serviceImpl;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import blinkstay.auth.dtos.AddUserDto;
import blinkstay.auth.dtos.ImageUploadResult;
import blinkstay.auth.dtos.TempUserRegistrationData;
import blinkstay.auth.dtos.UserResponseDto;
import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;
import blinkstay.auth.exceptionhandler.UserAlreadyExistsException;
import blinkstay.auth.exceptionhandler.UserNotFoundException;
import blinkstay.auth.mapper.ModelMapper;
import blinkstay.auth.repository.UserRepository;
import blinkstay.auth.service.ImageUploadService;
import blinkstay.auth.service.UserService;
import blinkstay.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;

	@Qualifier("random")
	private final SecureRandom random;

	@Qualifier("otpHolder")
	private final Map<String, TempUserRegistrationData> otpHolder;

	private final NotificationService notificationService;

	private final ImageUploadService imageUploadService;

	private final PasswordEncoder passwordEncoder;

	private final ModelMapper modelMapper;

//	public UserServiceImpl(UserRepository userRepository, SecureRandom random,
//			Map<String, TempUserRegistrationData> otpHolder, NotificationService notificationService,
//			ImageUploadService imageUploadService,PasswordEncoder passwordEncoder) {
//		this.userRepository = userRepository;
//		this.otpHolder = otpHolder;
//		this.random = random;
//		this.notificationService = notificationService;
//		this.imageUploadService = imageUploadService;
//		this.passwordEncoder = passwordEncoder;
//	}

	@Override
	public User helperGetUserId(UUID userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
		return user;
	}

	@Override
	public UserResponseDto getUserById(UUID userId) {
		User user = helperGetUserId(userId);
		UserResponseDto userResponseDto = modelMapper.userToUserResponseDto(user);
		return userResponseDto;
	}

	@Override
	public UserResponseDto getUserByEmail(String userEmail) {
		User user = userRepository.findByEmail(userEmail)
				.orElseThrow(() -> new UserNotFoundException("User not found"));
		UserResponseDto userResponseDto = modelMapper.userToUserResponseDto(user);
		return userResponseDto;
	}

	@Override
	public String deleteUserByEmail(UUID userId) {
		User user = helperGetUserId(userId);

		user.setIsActive(false);
		return "User deleted";
	}

	@Override
	public List<UserResponseDto> getAllUsers() {
		List<User> users = userRepository.findAll();

		List<UserResponseDto> userResponseDtos = modelMapper.usersToResponseDtos(users);
		return userResponseDtos;
	}

	@Override
	public String initiateUserRegistration(AddUserDto addUserDto, MultipartFile profileImg) throws IOException {
		Optional<User> optUser = userRepository.findByEmail(addUserDto.getEmail());
		if (optUser.isPresent()) {
			throw new UserAlreadyExistsException("User with the same email already exits!");
		}

		// OTP generation
		Integer otp = random.nextInt(100000, 999999);
		String emailSubject = "User Registration OTP";

		// Image details
		byte[] imgBytes = profileImg.getBytes();
		String fileName = profileImg.getOriginalFilename();
		String contentType = profileImg.getContentType();

		// Call the notification module sendOtpUserRegistration()
		// notificationService.sendOtpUserRegistration(addUserDto.getUsername(),
		// addUserDto.getEmail(), emailSubject, String.valueOf(otp));

		// Storing the details temporarily
		TempUserRegistrationData tempData = TempUserRegistrationData.builder().otp(String.valueOf(otp))
				.expiryTime(LocalDateTime.now().plusMinutes(6)).userDto(addUserDto).imageBytes(imgBytes)
				.originalFilename(fileName).contentType(contentType).build();

		otpHolder.put(addUserDto.getEmail(), tempData);

		log.info("Temp registration data stored for email: {}", addUserDto.getEmail());
		log.info("Temp user OTP {}", otp);

		return "OTP sent successfully";
	}

	@Override
	public UserResponseDto verifyOtpAndRegister(String email, String otp) {
		TempUserRegistrationData tempData = otpHolder.get(email);
		log.info("OTP Holder: {}", otpHolder.size());

		if (tempData == null) {
			throw new RuntimeException("No registration request found");
		}

		if (tempData.getExpiryTime().isBefore(LocalDateTime.now())) {
			throw new RuntimeException("OTP expired");
		}

		if (!tempData.getOtp().equals(otp)) {
			throw new RuntimeException("Invalid OTP");
		}

		AddUserDto dto = tempData.getUserDto();
		String encodedPassword = passwordEncoder.encode(dto.getPassword());

		User user = User.builder().username(dto.getUsername()).email(dto.getEmail()).password(encodedPassword)
				.roles(Set.of(UserRole.USER)).build();

		if (tempData.getImageBytes() != null) {
			String public_id = "user_" + System.currentTimeMillis() + "_" + user.getEmail();
			ImageUploadResult result = imageUploadService.uploadImage(tempData.getImageBytes(), public_id);

			user.setProfileImgUrl(result.getUrl());
			user.setImagePublicId(result.getPublicId());
		}

		User saved = userRepository.save(user);

		// User registered notification
		// notificationService.sendMailUserRegistered(user.getUsername(),
		// user.getEmail(), "ALERT: User Registration");

		// Cleanup memory
		otpHolder.remove(email);

		UserResponseDto userResponseDto = modelMapper.userToUserResponseDto(saved);

		return userResponseDto;
	}

	@Override
	public Map<String, Object> getImageDetails(UUID userId) {
		User user = helperGetUserId(userId);
		if (user.getImagePublicId() != null) {
			Map<String, Object> details = imageUploadService.getImageDetails(user.getImagePublicId());
			return details;
		}

		throw new RuntimeException("User image is not uploaded");
	}

	// UPDATE profile image (replace existing)
	@Override
	public UserResponseDto updateProfileImage(UUID userId, MultipartFile image) {
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		// Delete old image from Cloudinary if exists
		if (user.getImagePublicId() != null || !user.getImagePublicId().isEmpty()) {
			imageUploadService.deleteImage(user.getImagePublicId());
		}

		// Upload new image
		String publicId = "user_" + userId + "_" + System.currentTimeMillis() + "_" + user.getEmail();
		ImageUploadResult result = imageUploadService.updateImage(image, publicId);

		user.setProfileImgUrl(result.getPublicId());
		user.setImagePublicId(result.getPublicId());

		user = userRepository.save(user);
		UserResponseDto userResponseDto = modelMapper.userToUserResponseDto(user);

		return userResponseDto;
	}

	// REPLACE image (keep same public_id, URL remains same)
	@Override
	public UserResponseDto replaceProfileImage(UUID userId, MultipartFile newImage) {
		User user = helperGetUserId(userId);

		if (user.getImagePublicId() == null) {
			ImageUploadResult result = imageUploadService.replaceImage(newImage, null);
			user.setProfileImgUrl(result.getUrl());
			user.setImagePublicId(result.getPublicId());
		} else {
			// Replace existing image using same public_id
			ImageUploadResult result = imageUploadService.replaceImage(newImage, user.getImagePublicId());
			user.setProfileImgUrl(result.getUrl());
			// public_id remains the same
		}

		user = userRepository.save(user);
		UserResponseDto userResponseDto = modelMapper.userToUserResponseDto(user);
		return userResponseDto;
	}

	// UPDATE image using overwrite (more efficient)
	@Override
	public UserResponseDto updateProfileImageEfficient(UUID userId, MultipartFile newImage) {
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		if (user.getImagePublicId() == null) {
			// Upload new
			ImageUploadResult result = imageUploadService.uploadImage(newImage, null);
			user.setProfileImgUrl(null);
			user.setImagePublicId(result.getPublicId());
		} else {
			// Update existing using overwrite parameter
			ImageUploadResult result = imageUploadService.updateImage(newImage, user.getImagePublicId());
			user.setProfileImgUrl(result.getUrl());
			// public_id remains the same
		}

		user = userRepository.save(user);

		UserResponseDto userResponseDto = modelMapper.userToUserResponseDto(user);
		return userResponseDto;
	}

	// DELETE user profile image
	@Override
	public UserResponseDto deleteProfileImage(UUID userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		if (user.getImagePublicId() != null) {
			// Delete from Cloudinary
			Map<String, Object> result = imageUploadService.deleteImage(user.getImagePublicId());

			if ("ok".equals(result.get("result"))) {
				user.setProfileImgUrl(null);
				user.setImagePublicId(null);
				user = userRepository.save(user);
			} else {
				throw new RuntimeException("Failed to delete image from Cloudinary");
			}
		}

		UserResponseDto userResponseDto = modelMapper.userToUserResponseDto(user);
		return userResponseDto;// No image to delete
	}

	// BULK DELETE - delete multiple user images
	@Override
	public void deleteMultipleUserImage(List<UUID> userIds) {
		List<User> users = userRepository.findAllById(userIds);

		for (User user : users) {
			try {
				imageUploadService.deleteImage(user.getImagePublicId());
				user.setImagePublicId(null);
				user.setProfileImgUrl(null);
			} catch (Exception e) {
				log.info("Failed to delete image for user: {} : {}", user.getId(), e.getMessage());
			}
		}
		userRepository.saveAll(users);
	}

}
