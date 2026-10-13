package blinkstay.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
	Optional<User> findByEmail(String email);

	@Query("SELECT u.id FROM User u JOIN u.roles r WHERE r = :role AND u.isActive = true")
	List<UUID> findUserIdsByRole(@Param("role") UserRole role);

	@Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r = :role AND u.isActive = true")
	long countActiveByRole(@Param("role") UserRole role);

	@Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r = :role AND u.isActive = true")
	List<User> findActiveByRole(@Param("role") UserRole role);
}
