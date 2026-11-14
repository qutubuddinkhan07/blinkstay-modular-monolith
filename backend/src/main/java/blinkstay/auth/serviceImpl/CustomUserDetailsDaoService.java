package blinkstay.auth.serviceImpl;

import blinkstay.auth.entities.User;
import blinkstay.auth.repository.UserRepository;
import blinkstay.auth.security.AuthUser.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsDaoService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Used by login (email) - always reads the database.
     */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String id = identifier == null ? "" : identifier.trim();

        User user = findUser(id).orElseThrow(() -> {
            log.warn("No user found login identifier");
            return new UsernameNotFoundException("User not found");
        });

        return toAuthUser(user);
    }

    /**
     * Used by the JWT filter on every request. Cached for a short time under
     * "authUsers". It MUST be evicted whenever a user is blocked, unblocked,
     * deleted, or has roles or password changed.
     */
    @Cacheable(value = "authUsers", key = "#userId")
    public UserDetails loadAuthSnapshot(String userId) {
        UUID id = UUID.fromString(userId); // bad value -> IllegalAgumentException -> TOKEN_INVALID
        User user = userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toAuthUser(user);
    }

    private AuthUser toAuthUser(User user) {
        List<SimpleGrantedAuthority> authorities =
                user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.name())).toList();

        return new AuthUser(user.getId().toString(), user.getPassword(), !Boolean.FALSE.equals(user.getIsActive()),
                authorities, user.getPasswordChangedAt());
    }

    private Optional<User> findUser(String id) {
        if (id.contains("@")) {
            return userRepository.findByEmail(id.toLowerCase()); // login with email
        }
        try {
            return userRepository.findById(UUID.fromString(id)); // JWT filter
        } catch (IllegalArgumentException e) {
            return userRepository.findByEmail(id.toLowerCase());
        }
    }
}