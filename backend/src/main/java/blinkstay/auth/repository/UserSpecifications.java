package blinkstay.auth.repository;

import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class UserSpecifications {
    private UserSpecifications() {
    }

    /**
     * deleted = false: live accounts only (the default). deleted = true: deleted
     * accounts only. For deleted accounts "blocked" means they were blocked before
     * the delete (blockedAt is set), because isActive is false for all of them.
     */
    public static Specification<User> adminSearch(String q, Boolean blocked, boolean deleted, UserRole role) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            Expression<Set<UserRole>> roles = root.get("roles");

            p.add(deleted ? cb.isNotNull(root.get("deletedAt")) : cb.isNull(root.get("deletedAt")));

            if (q != null) {
                String like = "%" + q.toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("username")), like), cb.like(cb.lower(root.get("email")), like)));
            }

            if (blocked != null) {
                if (deleted) {
                    p.add(blocked ? cb.isNotNull(root.get("blockedAt")) : cb.isNull(root.get("blockedAt")));
                } else {
                    p.add(cb.equal(root.get("isActive"), !blocked));
                }
            }

            if (role != null) {
                p.add(cb.isMember(role, roles));
                if (role == UserRole.USER) {
                    p.add(cb.isNotMember(UserRole.ADMIN, roles));
                    p.add(cb.isNotMember(UserRole.HOTEL_MANAGER, roles));
                }
            }

            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}