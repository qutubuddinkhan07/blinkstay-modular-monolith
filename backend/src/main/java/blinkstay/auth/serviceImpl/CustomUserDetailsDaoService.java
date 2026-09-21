package blinkstay.auth.serviceImpl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import blinkstay.auth.entities.User;
import blinkstay.auth.repository.UserRepository;

@Service
public class CustomUserDetailsDaoService implements UserDetailsService {
	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
		User user = null;

		// Trying loading by UUID (User by JWT Filter on incoming API calls)
		try {
			UUID userId = UUID.fromString(identifier);
			user = userRepository.findById(userId).orElse(null);
		} catch (IllegalArgumentException e) {
			// Not a valid UUID string, proceed to lookup by email
		}

		// 2. Fallback to lookup by Email (Used during Login authentication)
		if (user == null) {
			user = userRepository.findByEmail(identifier)
					.orElseThrow(() -> new UsernameNotFoundException("User not found"));
		}

		List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
				.map(role -> new SimpleGrantedAuthority(role.name())).toList();

		// Passing the user.getId().toString() so Spring security uses UUID as the
		// principal identifier
		return org.springframework.security.core.userdetails.User.withUsername(user.getId().toString())
				.password(user.getPassword()).authorities(authorities).disabled(!user.getIsActive()).build();
	}
}
