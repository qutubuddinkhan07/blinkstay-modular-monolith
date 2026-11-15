package blinkstay.review.dto;

import java.util.List;

/**
 * averageRating is null when there are no reviews (and in the host's own list).
 */
public record ReviewPage(List<ReviewResponse> content, int page, int size, long totalElements, int totalPages,
                         Double averageRating, long reviewCount) {
}