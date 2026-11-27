package blinkstay.common.image;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * The one way the app stores images. Files are stored exactly as uploaded:
 * no crop, resize or quality change (BLINK-18). Sizes for display come from the URL.
 */
public interface ImageStorage {
    /**
     * Checks the file (not empty, allowed type for the folder) before anything is sent: 400 otherwise.
     */
    ImageUploadResult upload(MultipartFile file, ImageFolder folder, String publicId);

    /**
     * For bytes kept in memory (registration waits for the OTP before uploading).
     */
    ImageUploadResult upload(byte[] bytes, ImageFolder folder, String publicId);

    /**
     * True when the file is gone afterwards (deleted now, or already missing).
     */
    boolean delete(String publicId);

    /**
     * Cloudinary's stored details of one image (size, format, dimensions).
     */
    Map<String, Object> details(String publicId);
}
