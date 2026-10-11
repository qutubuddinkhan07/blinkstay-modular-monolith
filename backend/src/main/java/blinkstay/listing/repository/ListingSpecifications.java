package blinkstay.listing.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import blinkstay.listing.entities.Listing;
import blinkstay.listing.enums.ListingCategory;
import blinkstay.listing.enums.ListingStatus;
import jakarta.persistence.criteria.Predicate;

public final class ListingSpecifications {
	private ListingSpecifications() {
	}

	public static Specification<Listing> publishedSearch(ListingStatus status, String location, String name,
			ListingCategory category) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			// 1. Status Filter
			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}

			// 2. Category Filter
			if (category != null) {
				predicates.add(cb.equal(root.get("category"), category));
			}

			// 3. Combined Location OR Title Search
			String searchTerm = (location != null && !location.isBlank()) ? location.trim() : name;
			if (searchTerm != null && !searchTerm.isBlank()) {
				String[] words = searchTerm.toLowerCase().split("\\s+");
				for (String word : words) {
					String likePattern = "%" + word + "%";
					Predicate locationMatch = cb.like(cb.lower(root.get("location")), likePattern);
					Predicate titleMatch = cb.like(cb.lower(root.get("title")), likePattern);

					// Match word in EITHER location OR title
					predicates.add(cb.or(locationMatch, titleMatch));
				}
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

}
