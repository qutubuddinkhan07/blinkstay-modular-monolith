package blinkstay.auth.dtos;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResponseDto<T> {
	private List<T> content;
	private int pageNumber;
	private int pageSize;
	private long totalElements;
	private int totalPages;
	private boolean last;
	private boolean first;

	// Helper method to map Spring's Page object to your custom DTO
	public static <T> PageResponseDto<T> of(Page<T> page) {
		return PageResponseDto.<T>builder().content(page.getContent()).pageNumber(page.getNumber())
				.pageSize(page.getSize()).totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
				.last(page.isLast()).first(page.isFirst()).build();
	}
}
