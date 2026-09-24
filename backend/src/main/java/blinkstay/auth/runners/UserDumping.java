package blinkstay.auth.runners;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;
import blinkstay.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class UserDumping implements CommandLineRunner {
	private final UserRepository userRepo;
	private final PasswordGenerator passwordGenerator;

	@Transactional
	@Override
	public void run(String... args) throws Exception {
		int countUser = (int) userRepo.count();

		String pass = "123456789";
		String password = passwordGenerator.generatePassword(pass);
		String adminUsername = "Naruto";
		String hotelManagerUsername = "Hinata";
		String normalUser = "Boruto";

		String adminEmail = "naruto123@example.com";
		String hotelManagerEmail = "hinata123@example.com";
		String normalEmail = "boruto123@example.com";

		Set<UserRole> adminRole = Set.of(UserRole.USER, UserRole.ADMIN, UserRole.HOTEL_MANAGER);
		Set<UserRole> hotelManagerRole = Set.of(UserRole.USER, UserRole.HOTEL_MANAGER);
		Set<UserRole> normalRole = Set.of(UserRole.USER);

		String adminImgUrl = "https://res.cloudinary.com/dps1vvchb/image/upload/v1790018317/naruto_es1lqx.jpg";
		String hotelManagerImgUrl = "https://res.cloudinary.com/dps1vvchb/image/upload/v1790018318/Hinata_Hyuga_sax96p.jpg";
		String normalUserprofileImgUrl = "https://res.cloudinary.com/dps1vvchb/image/upload/v1790018318/boruto_cofygy.jpg";
		if (countUser < 1) {

			User admin = User.builder().username(adminUsername).email(adminEmail).password(password).roles(adminRole)
					.profileImgUrl(adminImgUrl).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
			User hotelManager = User.builder().username(hotelManagerUsername).email(hotelManagerEmail)
					.profileImgUrl(hotelManagerImgUrl).password(password).roles(hotelManagerRole)
					.createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
			User user = User.builder().username(normalUser).email(normalEmail).password(password).roles(normalRole)
					.profileImgUrl(normalUserprofileImgUrl).createdAt(LocalDateTime.now())
					.updatedAt(LocalDateTime.now()).build();

			userRepo.save(admin);
			userRepo.save(hotelManager);
			userRepo.save(user);

			log.info("Admin created with: Name= " + adminUsername + " Email= " + adminEmail + " Password= " + password);
			log.info("Hotel manager created with: Name= " + hotelManagerUsername + " Email= " + hotelManagerEmail
					+ " Password= " + password);
			log.info("User created with: Name= " + normalUser + " Email= " + normalEmail + " Password= " + password);
		} else {
			log.info("Users are already present");
			log.info("Admin already present with: Name= " + adminUsername + " Email= " + adminEmail + " Password= "
					+ pass);
			log.info("Hotel manager already present with: Name= " + hotelManagerUsername + " Email= "
					+ hotelManagerEmail + " Password= " + pass);
			log.info(
					"User already present with: Name= " + normalUser + " Email= " + normalEmail + " Password= " + pass);
		}
	}
}
