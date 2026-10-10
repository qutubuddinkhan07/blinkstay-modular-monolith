package blinkstay.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByEmail(String email);

	@Query("SELECT u.id FROM User u JOIN u.roles r WHERE r = :role AND u.isActive = true")
	List<UUID> findUserIdsByRole(@Param("role") UserRole role);

	@Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r = :role AND u.isActive = true")
	long countActiveByRole(@Param("role") UserRole role);

	@Query("""
			SELECT u FROM User u
			WHERE u.deletedAt IS NULL
			  AND (:q IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
			                  OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
			  AND (:active IS NULL OR u.isActive = :active)
			  AND (:role IS NULL OR :role MEMBER OF u.roles)
			""")
	Page<User> search(@Param("q") String q, @Param("active") Boolean active, @Param("role") UserRole role,
			Pageable pageable);
}
