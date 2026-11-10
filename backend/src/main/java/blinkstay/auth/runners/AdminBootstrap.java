package blinkstay.auth.runners;

import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;
import blinkstay.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Creates the first admin from environment variables, only when the database has
 * no admin yet. Used on a fresh production database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class AdminBootstrap implements CommandLineRunner {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-email:}")
    private String email;

    @Value("${app.bootstrap.admin-username:Admin}")
    private String username;

    @Value("${app.bootstrap.admin-password:}")
    private String password;

    @Override
    @Transactional
    public void run(String... args) {
        if (email.isBlank() || password.isBlank()) {
            return; // not configured: nothing to do
        }
        if (userRepo.countActiveByRole(UserRole.ADMIN) > 0) {
            log.info("An admin already exists, bootstrap admin not created");
            return;
        }
        if (password.length() < 10) {
            throw new IllegalStateException("app.bootstrap.admin-password must be at least 10 characters");
        }

        String normalized = email.trim().toLowerCase();
        if (userRepo.findByEmail(normalized).isPresent()) {
            throw new IllegalStateException("The bootstrap admin email already belongs to a non-admin user");
        }

        // ADMIN + HOTEL_MANAGER so the first admin can also own seeded demo listings
        Set<UserRole> roles = new HashSet<>(Set.of(UserRole.USER, UserRole.ADMIN, UserRole.HOTEL_MANAGER));
        userRepo.save(User.builder().username(username.trim()).email(normalized)
                .password(passwordEncoder.encode(password)).roles(roles).createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build());

        log.info("Bootstrap admin created for {}", normalized);
    }
}