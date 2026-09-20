package blinkstay.auth.serviceImpl;

import java.util.List;
import java.util.Optional;

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
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Optional<User> optUser = userRepository.findByEmail(email);

		if (optUser.isEmpty()) {
			throw new UsernameNotFoundException("User not found");
		}

		User user = optUser.get();

		List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
				.map(role -> new SimpleGrantedAuthority(role.name())).toList();

		return org.springframework.security.core.userdetails.User.withUsername(user.getEmail()) // or user.getUsername()
				.password(user.getPassword()).authorities(authorities).disabled(!user.getIsActive()) // Good practice to
																										// pass account
																										// state
				.build();
	}
}
