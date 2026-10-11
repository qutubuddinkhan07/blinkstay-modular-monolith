package blinkstay.auth.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import blinkstay.auth.entities.User;
import blinkstay.auth.enums.UserRole;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

public final class UserSpecifications {
	private UserSpecifications() {
	}

	public static Specification<User> adminSearch(String q, Boolean active, UserRole role) {
		return (root, query, cb) -> {
			List<Predicate> p = new ArrayList<>();
			Expression<Set<UserRole>> roles = root.get("roles");

			p.add(cb.isNull(root.get("deletedAt")));

			if (q != null) {
				String like = "%" + q.toLowerCase() + "%";
				p.add(cb.or(cb.like(cb.lower(root.get("username")), like), cb.like(cb.lower(root.get("email")), like)));
			}

			if (active != null) {
				p.add(cb.equal(root.get("isActive"), active));
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
