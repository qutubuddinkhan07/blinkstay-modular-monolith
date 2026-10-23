package blinkstay.booking.repository;

import blinkstay.booking.entities.Booking;
import blinkstay.booking.enums.BookingStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public static Specification<Booking> forHost(UUID hostId, List<BookingStatus> statuses, UUID listingId,
                                                 LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("hostId"), hostId));
            // a host never sees unpaid holds: only bookings that were paid at some point
            p.add(cb.isNotNull(root.get("confirmedAt")));
            common(root, cb, p, statuses, listingId, from, to);
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    public static Specification<Booking> forAdmin(String q, UUID guestId, UUID hostId, List<BookingStatus> statuses,
                                                  UUID listingId, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                p.add(cb.like(cb.upper(root.get("reference")), "%" + q.trim().toUpperCase() + "%"));
            }
            if (guestId != null) p.add(cb.equal(root.get("guestId"), guestId));
            if (hostId != null) p.add(cb.equal(root.get("hostId"), hostId));
            common(root, cb, p, statuses, listingId, from, to);
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static void common(Root<Booking> root, CriteriaBuilder cb, List<Predicate> p, List<BookingStatus> statuses,
                               UUID listingId, LocalDate from, LocalDate to) {
        if (statuses != null && !statuses.isEmpty()) p.add(root.get("status").in(statuses));
        if (listingId != null) p.add(cb.equal(root.get("listingId"), listingId));
        if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("checkIn"), from));
        if (to != null) p.add(cb.lessThanOrEqualTo(root.get("checkIn"), to));
    }
}
