package blinkstay.auth.serviceImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import blinkstay.auth.dtos.AddUserDto;
import blinkstay.auth.dtos.ImageUploadResult;
import blinkstay.auth.dtos.PageResponseDto;
import blinkstay.auth.dtos.TempUserRegistrationData;
import blinkstay.auth.dtos.UpdateProfileDto;
import blinkstay.auth.dtos.UserResponseDto;
import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;
import blinkstay.auth.mapper.ModelMapper;
import blinkstay.auth.repository.UserRepository;
import blinkstay.auth.service.ImageUploadService;
import blinkstay.auth.service.UserService;
import blinkstay.common.events.UserBlockedEvent;
import blinkstay.common.exception.UserAlreadyExistsException;
import blinkstay.common.exception.UserNotFoundException;
import blinkstay.notification.event.SimpleMessageEvent;
import blinkstay.notification.util.EmailMessageBuilderUtil;
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

	@Qualifier("authImageUploadService")
	private final ImageUploadService imageUploadService;

	private final PasswordEncoder passwordEncoder;

	@Qualifier("authModelMapper")
	private final ModelMapper modelMapper;

	private final ApplicationEventPublisher publisher;

	private final EmailMessageBuilderUtil emailMessageBuilderUtil;

	private static final int OTP_VALID_MINUTES = 6;
	private static final int OTP_RESEND_SECONDS = 60;
	private static final int OTP_MAX_ATTEMPTS = 5;

	@Override
	public User helperGetUserId(UUID userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));
		return user;
	}

	// CACHE USER LOOKUP BY ID
	@Override
	@Cacheable(value = "usersById", key = "#userId")
	public UserResponseDto getUserById(UUID userId) {
		User user = helperGetUserId(userId);
		UserResponseDto userResponseDto = modelMapper.userToUserResponseDto(user);
		return userResponseDto;
	}

	// CACHE USER LOOKUP BY EMAIL
	@Override
	@Cacheable(value = "usersByEmail", key = "#userEmail")
	public UserResponseDto getUserByEmail(String userEmail) {
		User user = userRepository.findByEmail(userEmail)
				.orElseThrow(() -> new UserNotFoundException("User not found"));
		UserResponseDto userResponseDto = modelMapper.userToUserResponseDto(user);
		return userResponseDto;
	}

	// EVICT CACHE ON SOFT DELETE
	@Override
	@Transactional
	@Caching(evict = { @CacheEvict(value = "usersById", key = "#userId"),
			@CacheEvict(value = "usersByEmail", allEntries = true),
			@CacheEvict(value = "hotelManagerIds", allEntries = true) })
	public String deleteUserById(UUID userId) {
		User user = helperGetUserId(userId);

		if (user.getDeletedAt() != null) {
			throw new UserNotFoundException("User not found");
		}
		if (user.getRoles().contains(UserRole.ADMIN) && Boolean.TRUE.equals(user.getIsActive())
				&& userRepository.countActiveByRole(UserRole.ADMIN) <= 1) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "The last active admin cannot be deleted");
		}

		user.setIsActive(false);
		user.setDeletedAt(LocalDateTime.now());
		userRepository.save(user);

		publisher.publishEvent(new UserBlockedEvent(userId, "Account deleted"));
		return "User deleted";
	}

	@Override
	public PageResponseDto<UserResponseDto> getAllUsers(Pageable pageable) {
		Page<User> userPage = userRepository.findAll(pageable);

		// Map User entities to UserReponseDto
		Page<UserResponseDto> userResponseDtoPage = userPage.map(modelMapper::userToUserResponseDto);

		// Convert to custom PageResponseDto
		return PageResponseDto.of(userResponseDtoPage);
	}

	@Override
	public String initiateUserRegistration(AddUserDto addUserDto, MultipartFile profileImg) throws IOException {
		String email = normalizeEmail(addUserDto.getEmail());

		if (userRepository.findByEmail(email).isPresent()) {
			throw new UserAlreadyExistsException("User with the same email already exists!");
		}

		TempUserRegistrationData exisiting = otpHolder.get(email);
		if (exisiting != null && exisiting.getExpiryTime().minusMinutes(OTP_VALID_MINUTES)
				.plusSeconds(OTP_RESEND_SECONDS).isAfter(LocalDateTime.now())) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"A code was just sent. Please wait a minute before requesting another.");
		}

		String otp = String.valueOf(random.nextInt(100000, 1000000));

		TempUserRegistrationData tempData = TempUserRegistrationData.builder().otp(otp)
				.expiryTime(LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES)).userDto(addUserDto)
				.imageBytes(profileImg.getBytes()).originalFilename(profileImg.getOriginalFilename())
				.contentType(profileImg.getContentType()).build();

		otpHolder.put(email, tempData);

		// TODO Step 6: send by email, then delete this log line
		log.info("Registration OTP for {}: {}", email, otp);

		// User otp email
		String message = emailMessageBuilderUtil.otpMessageBuilder(addUserDto.getUsername(), addUserDto.getEmail(),
				otp);
		SimpleMessageEvent event = SimpleMessageEvent.builder().receiverId(addUserDto.getEmail())
				.subject("ALERT: User Registration").message(message).build();
		publisher.publishEvent(event);

		return "OTP sent successfully";
	}

	@Override
	public UserResponseDto verifyOtpAndRegister(String email, String otp) {
		String key = normalizeEmail(email);

		TempUserRegistrationData tempData = otpHolder.get(key);

		if (tempData == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No registration request found");
		}

		if (tempData.getExpiryTime().isBefore(LocalDateTime.now())) {
			otpHolder.remove(key);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP expired");
		}

		if (tempData.getAttempts().incrementAndGet() > OTP_MAX_ATTEMPTS) {
			otpHolder.remove(key);
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"Too many wrong codes. Please start registration again.");
		}

		String given = otp == null ? "" : otp.trim();
		if (!MessageDigest.isEqual(tempData.getOtp().getBytes(StandardCharsets.UTF_8),
				given.getBytes(StandardCharsets.UTF_8))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
		}

		// the email may have been taken since the code was requested
		if (userRepository.findByEmail(key).isPresent()) {
			otpHolder.remove(key);
			throw new UserAlreadyExistsException("User with the same email already exits!");
		}

		AddUserDto dto = tempData.getUserDto();
		User user = User.builder().username(dto.getUsername()).email(key)
				.password(passwordEncoder.encode(dto.getPassword())).roles(new HashSet<>(Set.of(UserRole.USER)))
				.build();

		if (tempData.getImageBytes() != null) {
			String publicId = "user_" + System.currentTimeMillis(); // never put the email in a public URL
			ImageUploadResult result = imageUploadService.uploadImage(tempData.getImageBytes(), publicId);
			user.setProfileImgUrl(result.getUrl());
			user.setImagePublicId(result.getPublicId());
		}

		User saved;
		try {
			saved = userRepository.save(user);

			// TODO Step 6: send by email, then delete this log line
			log.info("Registration successfull for {}", email);

			// User registered email
			String message = emailMessageBuilderUtil.userRegisteredMessageBuilder(user.getUsername());
			SimpleMessageEvent event = SimpleMessageEvent.builder().receiverId(user.getEmail())
					.subject("User Registration Successful").message(message).build();
			publisher.publishEvent(event);
		} catch (DataIntegrityViolationException e) {
			// two verifications at once: the unique email constraint let only one through
			if (user.getImagePublicId() != null) {
				try {
					imageUploadService.deleteImage(user.getImagePublicId());
				} catch (Exception ex) {
					log.warn("Could not remove the orphaned image {}", user.getImagePublicId());
				}
			}
			otpHolder.remove(key);
			throw new UserAlreadyExistsException("User with the same email already exits!");
		}

		otpHolder.remove(key);
		return modelMapper.userToUserResponseDto(saved);
	}

	private static String normalizeEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase();
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

	// EVICT CACHE ON PROFILE IMAGE UPDATE
	@Override
	@Transactional
	@Caching(evict = { @CacheEvict(value = "usersById", key = "#userId"),
			@CacheEvict(value = "usersByEmail", allEntries = true) })
	public UserResponseDto updateProfileImage(UUID userId, MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw new IllegalArgumentException("Image is required");
		}

		String type = image.getContentType();
		if (type == null || !type.startsWith("image/")) {
			throw new IllegalArgumentException("File must be an image");
		}

		User user = helperGetUserId(userId);
		String oldPublicId = user.getImagePublicId();

		// Upload new image
		String publicId = "user_" + userId + "_" + System.currentTimeMillis();
		ImageUploadResult result = imageUploadService.updateImage(image, publicId);

		user.setProfileImgUrl(result.getUrl());
		user.setImagePublicId(result.getPublicId());

		user = userRepository.save(user);

		if (oldPublicId != null && !oldPublicId.isBlank()) {
			try {
				imageUploadService.deleteImage(oldPublicId);
			} catch (Exception e) {
				log.warn("Could not delete old image {}: {}", oldPublicId, e.getMessage());
			}
		}

		return modelMapper.userToUserResponseDto(user);
	}

	// EVICT CACHE ON IMAGE DELETE
	@Override
	@Transactional
	@Caching(evict = { @CacheEvict(value = "usersById", key = "#userId"),
			@CacheEvict(value = "usersByEmail", key = "#result.email", condition = "#result != null") })
	public UserResponseDto deleteProfileImage(UUID userId) {
		User user = helperGetUserId(userId); // throws UserNotFoundException

		String publicId = user.getImagePublicId();
		if (publicId == null || publicId.isBlank()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					"You don't have a profile image publicId [Seeded data] to delete");
		}

		Map<String, Object> result = imageUploadService.deleteImage(publicId);
		String outcome = String.valueOf(result.get("result"));

		// "not found" means Cloudinary no longer has it, so clearing our record is
		// correct
		if (!"ok".equals(outcome) && !"not found".equals(outcome)) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not delete the image, please try again");
		}

		user.setProfileImgUrl(null);
		user.setImagePublicId(null);
		return modelMapper.userToUserResponseDto(userRepository.save(user));
	}

	// BULK DELETE - delete multiple user images
	// EVICT CACHE IN BULK
	@Override
	@Transactional
	@Caching(evict = { @CacheEvict(value = "usersById", allEntries = true),
			@CacheEvict(value = "usersByEmail", allEntries = true) })
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

	// CACHE APPROVED HOTEL MANAGERS LIST
	@Override
	@Cacheable(value = "hotelManagerIds")
	public List<UUID> getApprovedHotelManagerIds() {
		return userRepository.findUserIdsByRole(UserRole.HOTEL_MANAGER);
	}

	@Override
	@Transactional
	@Caching(evict = { @CacheEvict(value = "usersById", key = "#userId"),
			@CacheEvict(value = "usersByEmail", allEntries = true) })
	public UserResponseDto updateProfile(UUID userId, UpdateProfileDto dto) {
		User user = helperGetUserId(userId);

		if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
			user.setUsername(dto.getUsername().trim());
		}
		if (dto.getPhone() != null) {
			user.setPhone(dto.getPhone().isBlank() ? null : dto.getPhone().trim());
		}
		if (dto.getBio() != null) {
			user.setBio(dto.getBio().isBlank() ? null : dto.getBio().trim());
		}

		return modelMapper.userToUserResponseDto(userRepository.save(user));
	}

	@Override
	@Transactional
	public void changePassword(UUID userId, String currentPassword, String newPassword) {
		User user = helperGetUserId(userId);
		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
			throw new IllegalArgumentException("Current password is incorrect");
		}
		user.setPassword(passwordEncoder.encode(newPassword));
		userRepository.save(user);
	}

}
