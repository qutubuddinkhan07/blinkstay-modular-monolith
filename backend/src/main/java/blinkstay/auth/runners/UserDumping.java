package blinkstay.auth.runners;

import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;
import blinkstay.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Development only: creates three sample users on an empty database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
@Profile("dev")
public class UserDumping implements CommandLineRunner {
    private final UserRepository userRepo;
    private final PasswordGenerator passwordGenerator;

    @Transactional
    @Override
    public void run(String... args) throws Exception {
        if (userRepo.count() > 0) {
            log.info("Users are already present, dev users not created");
            return;
        }

        String password = passwordGenerator.generatePassword("123456789");

        Set<UserRole> adminRole = Set.of(UserRole.USER, UserRole.ADMIN, UserRole.HOTEL_MANAGER);
        Set<UserRole> hotelManagerRole = Set.of(UserRole.USER, UserRole.HOTEL_MANAGER);
        Set<UserRole> normalRole = Set.of(UserRole.USER);

        String adminImgUrl = "https://res.cloudinary.com/dps1vvchb/image/upload/v1790018317/naruto_es1lqx.jpg";
        String hotelManagerImgUrl = "https://res.cloudinary.com/dps1vvchb/image/upload/v1790018318/Hinata_Hyuga_sax96p.jpg";
        String normalUserprofileImgUrl = "https://res.cloudinary.com/dps1vvchb/image/upload/v1790018318/boruto_cofygy.jpg";

        userRepo.save(User.builder().username("Naruto").email("qutubuddinkhan8261@gmail.com").password(password)
                .roles(adminRole).profileImgUrl(adminImgUrl).createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build());
        userRepo.save(User.builder().username("Hinata").email("hinata123@example.com").password(password)
                .roles(hotelManagerRole).profileImgUrl(hotelManagerImgUrl).createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build());
        userRepo.save(User.builder().username("Boruto").email("boruto123@example.com").password(password)
                .roles(normalRole).profileImgUrl(normalUserprofileImgUrl).createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).build());

        log.info("Dev users created: naruto123@example.com (admin), hinata123@example.com (host), "
                + "boruto123@example.com (user). Password is set in UserDumping.");
    }
}