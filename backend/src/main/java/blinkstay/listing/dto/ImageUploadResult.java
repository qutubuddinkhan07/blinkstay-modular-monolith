package blinkstay.listing.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImageUploadResult {
	private String url;
	private String publicId;
	private Long size;
	private String format;
}
