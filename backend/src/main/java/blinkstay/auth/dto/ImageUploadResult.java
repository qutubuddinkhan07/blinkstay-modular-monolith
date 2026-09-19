package blinkstay.auth.dto;

import lombok.*;

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
