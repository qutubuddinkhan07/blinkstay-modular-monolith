package com.blinkstay.auth.serviceImpl;

import com.blinkstay.auth.entities.User;
import com.blinkstay.auth.repository.UserRepository;
import com.blinkstay.auth.security.AuthUser.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsDaoService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Used by login only (the "username" is the email). Always reads the database.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String key = email == null ? "" : email.trim().toLowerCase();

        User user = userRepository.findByEmail(key).orElseThrow(() -> {
            log.warn("No user found for the login email");
            return new UsernameNotFoundException("User not found");
        });

        return toAuthUser(user);
    }

    /**
     * Used by the JWT filter on every request. Cached for a short time under
     * "authUsers". It MUST be evicted whenever a user is blocked, unblocked,
     * deleted, restored, or has roles or password changed.
     */
    @Cacheable(value = "authUsers", key = "#userId")
    public UserDetails loadAuthSnapshot(String userId) {
        UUID id = UUID.fromString(userId); // bad value -> IllegalArgumentException -> TOKEN_INVALID
        User user = userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toAuthUser(user);
    }

    private AuthUser toAuthUser(User user) {
        List<SimpleGrantedAuthority> authorities =
                user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.name())).toList();

        // enabled = active AND not deleted (a deleted row must never log in, even if isActive was edited by hand)
        boolean enabled = !Boolean.FALSE.equals(user.getIsActive()) && user.getDeletedAt() == null;

        return new AuthUser(user.getId().toString(), user.getPassword(), enabled, authorities,
                user.getPasswordChangedAt());
    }
}