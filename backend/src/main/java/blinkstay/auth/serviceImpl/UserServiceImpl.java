package blinkstay.auth.serviceImpl;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import blinkstay.auth.dto.AddUserDto;
import blinkstay.auth.dto.ImageUploadResult;
import blinkstay.auth.dto.TempUserRegistrationData;
import blinkstay.auth.dto.UserResponseDto;
import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;
import blinkstay.auth.exceptionhandler.UserAlreadyExistsException;
import blinkstay.auth.exceptionhandler.UserNotFoundException;
import blinkstay.auth.repository.UserRepository;
import blinkstay.auth.service.ImageUploadService;
import blinkstay.auth.service.UserService;
import blinkstay.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;

	@Qualifier("random")
	private final SecureRandom random;

	@Qualifier("otpHolder")
	private final Map<String, TempUserRegistrationData> otpHolder;

	private final NotificationService notificationService;

	private final ImageUploadService imageUploadService;

	public UserServiceImpl(UserRepository userRepository, SecureRandom random,
			Map<String, TempUserRegistrationData> otpHolder, NotificationService notificationService,
			ImageUploadService imageUploadService) {
		this.userRepository = userRepository;
		this.otpHolder = otpHolder;
		this.random = random;
		this.notificationService = notificationService;
		this.imageUploadService = imageUploadService;
	}

	@Override
	public User getUserById(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
		return user;
	}

	@Override
	public User getUserByEmail(String userEmail) {
		User user = userRepository.findByEmail(userEmail)
				.orElseThrow(() -> new UserNotFoundException("User not found"));
		return user;
	}

	@Override
	public List<User> getAllUsers() {
		List<User> users = userRepository.findAll();
		return users;
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
		User user = User.builder().username(dto.getUsername()).email(dto.getEmail()).password(dto.getPassword())
				.role(UserRole.USER).build();

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

		UserResponseDto userResponseDto = UserResponseDto.builder().id(saved.getId()).username(saved.getUsername())
				.email(saved.getEmail()).role(saved.getRole()).isACtive(saved.getIsActive())
				.profileImgUrl(saved.getProfileImgUrl()).createdAt(saved.getCreatedAt()).updatedAt(saved.getUpdatedAt())
				.build();

		return userResponseDto;
	}

	@Override
	public Map<String, Object> getImageDetails(Long userId) {
		User user = getUserById(userId);
		if (user.getImagePublicId() != null) {
			Map<String, Object> details = imageUploadService.getImageDetails(user.getImagePublicId());
			return details;
		}

		throw new RuntimeException("User image is not uploaded");
	}

	// UPDATE profile image (replace existing)
	@Override
	public User updateProfileImage(Long userId, MultipartFile image) {
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

		return userRepository.save(user);
	}

	// REPLACE image (keep same public_id, URL remains same)
	@Override
	public User replaceProfileImage(Long userId, MultipartFile newImage) {
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

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

		return userRepository.save(user);
	}

	// UPDATE image using overwrite (more efficient)
	@Override
	public User updateProfileImageEfficient(Long userId, MultipartFile newImage) {
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

		return userRepository.save(user);
	}

	// DELETE user profile image
	@Override
	public User deleteProfileImage(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		if (user.getImagePublicId() != null) {
			// Delete from Cloudinary
			Map<String, Object> result = imageUploadService.deleteImage(user.getImagePublicId());

			if ("ok".equals(result.get("result"))) {
				user.setProfileImgUrl(null);
				user.setImagePublicId(null);
				return userRepository.save(user);
			} else {
				throw new RuntimeException("Failed to delete image from Cloudinary");
			}
		}

		return user; // No image to delete
	}

	// BULK DELETE - delete multiple user images
	@Override
	public void deleteMultipleUserImage(List<Long> userIds) {
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
