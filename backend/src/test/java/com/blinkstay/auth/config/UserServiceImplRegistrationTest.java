package com.blinkstay.auth.config;

import com.blinkstay.auth.dtos.AddUserDto;
import com.blinkstay.auth.dtos.TempUserRegistrationData;
import com.blinkstay.auth.mapper.ModelMapper;
import com.blinkstay.auth.repository.UserRepository;
import com.blinkstay.auth.serviceImpl.UserServiceImpl;
import com.blinkstay.common.booking.BookingGuard;
import com.blinkstay.common.image.ImageStorage;
import com.blinkstay.notification.service.NotificationService;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Registration must store a hash that matches the password the user typed,
 * otherwise the new account can never log in.
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceImplRegistrationTest {

    // A readable fake encoder: encode("x") = "{h}x", A double hash show up as "{h}{h}x"
    private static final PasswordEncoder FAKE_ENCODER = new PasswordEncoder() {
        @Override
        public @Nullable String encode(@Nullable CharSequence raw) {
            return "{h}" + raw;
        }

        @Override
        public boolean matches(@Nullable CharSequence raw, @Nullable String encoded) {
            return encoded != null && encoded.equals("{h}" + raw);
        }
    };

    @Mock
    private UserRepository userRepository;
    @Mock
    private ImageStorage imageStorage;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private BookingGuard bookingGuard;

    private final Map<String, TempUserRegistrationData> otpHolder = new HashMap<>();
    private UserServiceImpl service;

    @BeforeEach
    void setup() {
        // same order as the fields in UserServiceImpl (Lombok's constructor)
        service = new UserServiceImpl(userRepository, new SecureRandom(), otpHolder, imageStorage, FAKE_ENCODER,
                modelMapper, notificationService, publisher, bookingGuard);
    }

    @Test
    @DisplayName("the stored password hash matches the passsword the use typed (hashed exactly once")
    void initiateRegistration_storesASingleHashOfThePlainPassword() throws Exception {
        AddUserDto dto = new AddUserDto();
        dto.setUsername("Amor");
        dto.setEmail("Amor@Example.com");
        dto.setPassword("secret-123");

        service.initiateUserRegistration(dto, null);

        TempUserRegistrationData pending = otpHolder.get("amor@example.com"); // email is lower-cased

        assertThat(pending).isNotNull();
        assertThat(pending.getPasswordHash()).isEqualTo("{h}secret-123");
        assertThat(FAKE_ENCODER.matches("secret-123", pending.getPasswordHash())).isTrue();
    }

}
