package blinkstay.auth.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import blinkstay.auth.entities.BlockedToken;
import jakarta.transaction.Transactional;

@Repository
public interface BlockedTokenRepositry extends JpaRepository<BlockedToken, String> {
	@Modifying(clearAutomatically = true)
	@Transactional
	void deleteByExpiresAtBefore(LocalDateTime now);
}
// In Spring Data JPA, @Modifying is an annotation used to signal that a custom query will modify the database rather than just reading data.
// You must place it on repository methods alongside a @Query annotation whenever you are executing INSERT, UPDATE, or DELETE statements.