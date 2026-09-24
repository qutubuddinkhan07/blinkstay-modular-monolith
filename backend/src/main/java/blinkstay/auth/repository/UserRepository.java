package blinkstay.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByEmail(String email);

	@Query("SELECT u.id FROM User u JOIN u.roles r WHERE r = :role")
	List<UUID> findBUserIdsByRole(@Param("role") UserRole role);
}
