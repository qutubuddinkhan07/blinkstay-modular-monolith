package blinkstay.booking.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import blinkstay.booking.entities.Booking;
import blinkstay.booking.enums.BookingStatus;
import jakarta.persistence.criteria.Predicate;

public final class BookingSpecifications {
	private BookingSpecifications() {
	}

	public static Specification<Booking> forGuest(UUID guestId, List<BookingStatus> statuses) {
		return (root, query, cb) -> {
			List<Predicate> p = new ArrayList<>();
			p.add(cb.equal(root.get("guestId"), guestId));
			if (statuses != null && !statuses.isEmpty()) {
				p.add(root.get("status").in(statuses));
			}

			return cb.and(p.toArray(new Predicate[0]));
		};
	}
}
