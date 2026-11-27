package com.blinkstay.auth.serviceImpl;

import com.blinkstay.auth.dtos.*;
import com.blinkstay.auth.entities.User;
import com.blinkstay.auth.enums.UserRole;
import com.blinkstay.auth.mapper.ModelMapper;
import com.blinkstay.auth.repository.UserRepository;
import com.blinkstay.auth.service.UserService;
import com.blinkstay.common.booking.BookingGuard;
import com.blinkstay.common.events.HostDetailsChangedEvent;
import com.blinkstay.common.events.UserDeletedEvent;
import com.blinkstay.common.exception.UserAlreadyExistsException;
import com.blinkstay.common.exception.UserNotFoundException;
import com.blinkstay.common.image.ImageFolder;
import com.blinkstay.common.image.ImageStorage;
import com.blinkstay.common.image.ImageUploadResult;
import com.blinkstay.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
    private final ImageStorage imageStorage;

    private final PasswordEncoder passwordEncoder;

    @Qualifier("authModelMapper")
    private final ModelMapper modelMapper;

    private final NotificationService notificationService;

    private final ApplicationEventPublisher publisher;

    private static final int OTP_VALID_MINUTES = 6;
    private static final int OTP_RESEND_SECONDS = 60;
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final BookingGuard bookingGuard;

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
    @Caching(evict = {@CacheEvict(value = "authUsers", allEntries = true),
            @CacheEvict(value = "usersById", key = "#userId"),
            @CacheEvict(value = "usersByEmail", allEntries = true),
            @CacheEvict(value = "hotelManagerIds", allEntries = true)})
    public String deleteUserById(UUID userId) {
        User user = helperGetUserId(userId);

        if (user.getDeletedAt() != null) {
            throw new UserNotFoundException("User not found");
        }
        if (user.getRoles().contains(UserRole.ADMIN) && Boolean.TRUE.equals(user.getIsActive())
                && userRepository.countActiveByRole(UserRole.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The last active admin cannot be deleted");
        }

        if (bookingGuard.guestHasActiveBookings(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The account has upcoming bookings. Cancel them first.");
        }

        if (bookingGuard.hostHasActiveBookings(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The account has upcoming guest bookings on its listings. Resolve them first.");
        }

        user.setIsActive(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        publisher.publishEvent(new UserDeletedEvent(userId, user.getRoles().contains(UserRole.HOTEL_MANAGER)));
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

        ensureEmailIsFree(email);

        TempUserRegistrationData exisiting = otpHolder.get(email);
        if (exisiting != null && exisiting.getExpiryTime().minusMinutes(OTP_VALID_MINUTES)
                .plusSeconds(OTP_RESEND_SECONDS).isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "A code was just sent. Please wait a minute before requesting another.");
        }

        String otp = String.valueOf(random.nextInt(100000, 1000000));

        boolean hasImage = profileImg != null && !profileImg.isEmpty();

        // never keep the plain password in memory while waiting for the OTP
        addUserDto.setPassword(passwordEncoder.encode(addUserDto.getPassword()));

        TempUserRegistrationData tempData = TempUserRegistrationData.builder().otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES))
                .username(addUserDto.getUsername())
                .passwordHash(addUserDto.getPassword()) // never keep the plain password
                .imageBytes(hasImage ? profileImg.getBytes() : null)
                .originalFilename(hasImage ? profileImg.getOriginalFilename() : null)
                .contentType(hasImage ? profileImg.getContentType() : null).build();

        otpHolder.put(email, tempData);

        notificationService.sendOtpUserRegistration(addUserDto.getUsername(), email, "Your Blinkstay verification code",
                otp, OTP_VALID_MINUTES);

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

        // the email may have been taken (or the account deleted) since the code was requested
        try {
            ensureEmailIsFree(key);
        } catch (RuntimeException e) {
            otpHolder.remove(key);
            throw e;
        }

        User user = User.builder().username(tempData.getUsername()).email(key)
                .password(tempData.getPasswordHash()).roles(new HashSet<>(Set.of(UserRole.USER)))
                .build();

        if (tempData.getImageBytes() != null) {
            String publicId = "user_" + System.currentTimeMillis(); // never put the email in a public URL
            ImageUploadResult result = imageStorage.upload(tempData.getImageBytes(), ImageFolder.PROFILE, publicId);
            user.setProfileImgUrl(result.url());
            user.setImagePublicId(result.publicId());
        }

        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // two verifications at once: the unique email constraint let only one through
            if (user.getImagePublicId() != null) {
                try {
                    imageStorage.delete(user.getImagePublicId());
                } catch (Exception ex) {
                    log.warn("Could not remove the orphaned image {}", user.getImagePublicId());
                }
            }
            otpHolder.remove(key);
            throw new UserAlreadyExistsException("User with the same email already exists!");
        }

        otpHolder.remove(key);

        notificationService.sendMailUserRegistered(saved.getUsername(), saved.getEmail(), "Welcome to Blinkstay");

        return modelMapper.userToUserResponseDto(saved);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    @Override
    public Map<String, Object> getImageDetails(UUID userId) {
        User user = helperGetUserId(userId);
        if (user.getImagePublicId() != null) {
            return imageStorage.details(user.getImagePublicId());
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User image is not uploaded");
    }

    // EVICT CACHE ON PROFILE IMAGE UPDATE
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "authUsers", allEntries = true),
            @CacheEvict(value = "usersById", key = "#userId"),
            @CacheEvict(value = "usersByEmail", allEntries = true)})
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
        ImageUploadResult result = imageStorage.upload(image, ImageFolder.PROFILE, publicId);

        user.setProfileImgUrl(result.url());
        user.setImagePublicId(result.publicId());

        user = userRepository.save(user);

        notifyUserDetailsChanged(user);

        if (oldPublicId != null && !oldPublicId.isBlank()) {
            try {
                imageStorage.delete(oldPublicId);
            } catch (Exception e) {
                log.warn("Could not delete old image {}: {}", oldPublicId, e.getMessage());
            }
        }

        return modelMapper.userToUserResponseDto(user);
    }

    // EVICT CACHE ON IMAGE DELETE
    @Override
    @Transactional
    @Caching(evict = {@CacheEvict(value = "authUsers", allEntries = true),
            @CacheEvict(value = "usersById", key = "#userId"),
            @CacheEvict(value = "usersByEmail", key = "#result.email", condition = "#result != null")})
    public UserResponseDto deleteProfileImage(UUID userId) {
        User user = helperGetUserId(userId); // throws UserNotFoundException

        String publicId = user.getImagePublicId();
        if (publicId == null || publicId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "You don't have a profile image publicId [Seeded data] to delete");
        }

        // true also for "not found": Cloudinary no longer has it, so clearing our record is correct
        if (!imageStorage.delete(publicId)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not delete the image, please try again");
        }

        user.setProfileImgUrl(null);
        user.setImagePublicId(null);

        User saved = userRepository.save(user);
        notifyUserDetailsChanged(saved);

        return modelMapper.userToUserResponseDto(saved);
    }

    // BULK DELETE - delete multiple user images
    // EVICT CACHE IN BULK
    @Override
    @Transactional
    @Caching(evict = {@CacheEvict(value = "authUsers", allEntries = true),
            @CacheEvict(value = "usersById", allEntries = true),
            @CacheEvict(value = "usersByEmail", allEntries = true)})
    public void deleteMultipleUserImage(List<UUID> userIds) {
        List<User> users = userRepository.findAllById(userIds);

        for (User user : users) {
            try {
                imageStorage.delete(user.getImagePublicId());
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
    @Caching(evict = {@CacheEvict(value = "authUsers", allEntries = true),
            @CacheEvict(value = "usersById", key = "#userId"),
            @CacheEvict(value = "usersByEmail", allEntries = true)})
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

        User saved = userRepository.save(user);
        notifyUserDetailsChanged(saved);

        return modelMapper.userToUserResponseDto(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "authUsers", allEntries = true)
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = helperGetUserId(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Entered new password same as current password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        userRepository.save(user);

        notificationService.sendPasswordChanged(user.getUsername(), user.getEmail(),
                "Your Blinkstay password was changed");
    }

    private void notifyUserDetailsChanged(User user) {
        // only hosts appear on listings, so a normal user editing their profile clears
        // nothing
        if (user.getRoles().contains(UserRole.HOTEL_MANAGER)) {
            publisher.publishEvent(new HostDetailsChangedEvent(user.getId()));
        }
    }

    /**
     * The email must not belong to any account, deleted ones included: a deleted
     * account keeps its email so it can be restored, never re-registered.
     */
    private void ensureEmailIsFree(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            if (u.getDeletedAt() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This email belongs to a deleted account. Contact support to restore it.");
            }
            throw new UserAlreadyExistsException("User with the same email already exists!");
        });
    }

}
