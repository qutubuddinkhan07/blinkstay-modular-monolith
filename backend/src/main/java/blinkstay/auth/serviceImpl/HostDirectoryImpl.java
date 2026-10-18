package blinkstay.auth.serviceImpl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import blinkstay.auth.entities.HotelManagerProfile;
import blinkstay.auth.repository.HotelManagerProfileRepository;
import blinkstay.auth.repository.UserRepository;
import blinkstay.common.directory.HostDirectory;
import blinkstay.common.directory.HostSummary;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HostDirectoryImpl implements HostDirectory {
	private final UserRepository userRepository;
	private final HotelManagerProfileRepository profileRepo;

	@Override
	@Transactional(readOnly = true)
	public Optional<HostSummary> findHost(UUID userId) {
		return userRepository.findById(userId).filter(u -> u.getDeletedAt() == null).map(u -> {
			HotelManagerProfile p = profileRepo.findByUserId(userId).orElse(null);

			String business = p == null ? null : blankToNull(p.getBusinessName());
			String about = p != null && blankToNull(p.getDescription()) != null ? p.getDescription() : u.getBio();

			return new HostSummary(u.getUsername(), business, u.getProfileImgUrl(), cut(blankToNull(about), 300),
					u.getCreatedAt() == null ? null : u.getCreatedAt().toLocalDate());
		});
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

	private static String cut(String s, int max) {
		return s == null || s.length() <= max ? s : s.substring(0, max - 1).trim() + "\u2026";
	}
}
