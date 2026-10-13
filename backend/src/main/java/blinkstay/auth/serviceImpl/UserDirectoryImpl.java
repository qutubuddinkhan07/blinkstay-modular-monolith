package blinkstay.auth.serviceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;
import blinkstay.auth.repository.UserRepository;
import blinkstay.common.directory.UserContact;
import blinkstay.common.directory.UserDirectory;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserDirectoryImpl implements UserDirectory {
	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public Optional<UserContact> findContact(UUID userId) {
		return userRepository.findById(userId).map(this::toContact);
	}

	@Override
	public List<UserContact> findActiveAdmins() {
		return userRepository.findActiveByRole(UserRole.ADMIN).stream().map(this::toContact).toList();
	}

	private UserContact toContact(User user) {
		return new UserContact(user.getId(), user.getUsername(), user.getEmail());
	}
}
