package blinkstay.listing.service;

import blinkstay.listing.dto.ImageUploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Only what the listing module really uses: upload a listing photo, delete one.
 */
public interface ImageUploadService {
    ImageUploadResult uploadImage(MultipartFile file, String customPublicId);

    Map<String, Object> deleteImage(String publicId);
}