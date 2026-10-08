package blinkstay.listing.dto;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<T> content;
	private int pageNumber;
	private int pageSize;
	private long totalElements;
	private int totalPages;
	private boolean last;
	private boolean first;

	public static <T> PagedResponse<T> from(Page<T> page) {
		return PagedResponse.<T>builder().content(page.getContent()).pageNumber(page.getNumber())
				.pageSize(page.getSize()).totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
				.last(page.isLast()).first(page.isFirst()).build();
	}
}
