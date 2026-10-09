package blinkstay.auth.serviceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import blinkstay.auth.entities.User;
import blinkstay.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsDaoService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
		String id = identifier == null ? "" : identifier.trim();

		User user = findUser(id).orElseThrow(() -> {
			log.warn("No user found for '{}'", id); // temporary: remove once login works
			return new UsernameNotFoundException("User not found");
		});

		List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
				.map(role -> new SimpleGrantedAuthority(role.name())).toList();

		return org.springframework.security.core.userdetails.User.withUsername(user.getId().toString())
				.password(user.getPassword()).authorities(authorities)
				.disabled(Boolean.FALSE.equals(user.getIsActive())).build();
	}

	private Optional<User> findUser(String id) {
		if (id.contains("@")) {
			return userRepository.findByEmail(id); // login with email
		}
		try {
			return userRepository.findById(UUID.fromString(id)); // JWT filter
		} catch (IllegalArgumentException e) {
			return userRepository.findByEmail(id);
		}
	}
}