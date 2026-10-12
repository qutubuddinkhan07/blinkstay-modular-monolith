package blinkstay.auth.serviceImpl;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import blinkstay.auth.entities.User;
import blinkstay.auth.repository.UserRepository;
import blinkstay.auth.service.PasswordResetService;
import blinkstay.auth.util.PasswordResetOtpStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

	private static final Duration COOLDOWN = Duration.ofSeconds(60);
	private static final int MAX_ATTEMPTS = 5;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordResetOtpStore store;

	@Qualifier("random")
	private final SecureRandom random;

	@Override
	@Transactional(readOnly = true)
	public void requestReset(String username, String email) {
		String key = normalize(email);

		// Generate and hash the code before looking at the account, so the response
		// takes about the same time whether or not the account exists.
		String otp = String.valueOf(random.nextInt(100000, 1000000));
		String otpHash = passwordEncoder.encode(otp);

		Optional<User> match = userRepository.findByEmail(email.trim()).filter(this::isEligible)
				.filter((u) -> u.getUsername().equalsIgnoreCase(username.trim()));

		if (match.isEmpty()) {
			log.info("Password reset requested for an unknown or ineligible account");
			return; // same response as success on purpose
		}

		PasswordResetOtpStore.Entry existing = store.get(key);
		if (existing != null && existing.createdAt().plus(COOLDOWN).isAfter(Instant.now())) {
			log.info("Password reset for {} ignored: cooldown active", key);
			return;
		}

		store.put(key, new PasswordResetOtpStore.Entry(otpHash));
		deliverOtp(match.get().getUsername(), key, otp);
	}

	@Override
	@Transactional
	public void resetPassword(String email, String otp, String newPassword) {
		String key = normalize(email);

		PasswordResetOtpStore.Entry entry = store.get(key);
		if (entry == null) {
			throw invalidCode();
		}
		if (entry.attempts().incrementAndGet() > MAX_ATTEMPTS) {
			store.remove(key); // too many guesses: the code is dead
			throw invalidCode();
		}
		if (!passwordEncoder.matches(otp.trim(), entry.otpHash())) {
			throw invalidCode();
		}

		User user = userRepository.findByEmail(email.trim()).filter(this::isEligible).orElseThrow(this::invalidCode);

		if (passwordEncoder.matches(newPassword, user.getPassword())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"The new password must be different from the current one");
		}

		user.setPassword(passwordEncoder.encode(newPassword));
		user.setPasswordChangedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));

		userRepository.save(user);
		store.remove(key);
		log.info("Password reset completed for user {}", user.getId());
	}

	// TODO Step 6: replace this log line with an email through the notification
	// module
	private void deliverOtp(String username, String email, String otp) {
		log.info("PASSWORD RESET OTP for {} ({}): {}", username, email, otp);
	}

	private boolean isEligible(User u) {
		return u.getDeletedAt() == null && Boolean.TRUE.equals(u.getIsActive());
	}

	private ResponseStatusException invalidCode() {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired code. Request a new one.");
	}

	private static String normalize(String email) {
		return email.trim().toLowerCase();
	}
}
