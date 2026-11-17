package blinkstay.auth.serviceImpl;

import blinkstay.auth.dtos.ImageUploadResult;
import blinkstay.auth.service.ImageUploadService;
import blinkstay.common.cloudinary.CloudinaryFolders;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Profile images are stored exactly as uploaded: no crop, no resize, no quality change.
 * Any resizing for display happens in the image URL or in CSS, never on the stored original.
 */
@Service("authImageUploadService")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"unchecked", "rawtypes"})
public class ImageUploadServiceImpl implements ImageUploadService {

    private static final String[] ALLOWED_FORMATS = {"jpg", "jpeg", "png", "gif"};

    private final Cloudinary cloudinary;
    private final CloudinaryFolders folders;

    @Override
    public ImageUploadResult uploadImage(MultipartFile file, String customPublicId) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload a valid image file.");
        }
        try {
            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("folder", folders.userProfiles());
            uploadParams.put("allowed_formats", ALLOWED_FORMATS);
            if (customPublicId != null && !customPublicId.isEmpty()) {
                uploadParams.put("public_id", customPublicId);
            }

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            return toResult(uploadResult);
        } catch (IOException e) {
            log.error("Profile image upload failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Image upload failed. Please try a different image.");
        }
    }

    @Override
    public ImageUploadResult uploadImage(byte[] imageBytes, String publicId) {
        try {
            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("folder", folders.userProfiles());
            uploadParams.put("public_id", publicId);

            Map uploadResult = cloudinary.uploader().upload(imageBytes, uploadParams);
            return toResult(uploadResult);
        } catch (IOException e) {
            throw new RuntimeException("Image upload failed", e);
        }
    }

    @Override
    public Map<String, Object> deleteImage(String publicId) {
        if (publicId == null || publicId.isEmpty()) {
            throw new IllegalArgumentException("Public ID is required for deletion");
        }
        try {
            // result is {"result": "ok"} or {"result": "not found"}
            return cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete image: " + e.getMessage(), e);
        }
    }

    @Override
    public ImageUploadResult replaceImage(MultipartFile newFile, String oldPublicId) {
        if (oldPublicId != null && !oldPublicId.isEmpty()) {
            deleteImage(oldPublicId);
        }
        return uploadImage(newFile, oldPublicId);
    }

    @Override
    public ImageUploadResult updateImage(MultipartFile newFile, String publicId) {
        if (newFile == null || newFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload a valid image file.");
        }
        if (!hasAllowedExtension(newFile.getOriginalFilename())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid image format. Allowed formats are: jpg, jpeg, png, gif.");
        }
        try {
            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("public_id", publicId);
            uploadParams.put("overwrite", true);
            uploadParams.put("invalidate", true); // old cached copy on the CDN is refreshed
            uploadParams.put("folder", folders.userProfiles());
            uploadParams.put("allowed_formats", ALLOWED_FORMATS);

            Map uploadResult = cloudinary.uploader().upload(newFile.getBytes(), uploadParams);
            return toResult(uploadResult);
        } catch (Exception e) {
            log.error("Profile image upload failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Image upload failed. Please try a different image.");
        }
    }

    @Override
    public Map<String, Object> getImageDetails(String publicId) {
        try {
            return cloudinary.api().resource(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get image details: " + e.getMessage(), e);
        }
    }

    // Builds a resized URL for display only; the stored original is never changed
    @Override
    public String getTransformedUrl(String publicId, int width, int height) {
        return cloudinary.url()
                .transformation(new Transformation<>().width(width).height(height).crop("fill").gravity("face"))
                .generate(publicId);
    }

    private static boolean hasAllowedExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        for (String allowed : ALLOWED_FORMATS) {
            if (allowed.equals(ext)) {
                return true;
            }
        }
        return false;
    }

    private static ImageUploadResult toResult(Map uploadResult) {
        Object bytes = uploadResult.get("bytes");
        long size = bytes instanceof Number n ? n.longValue()
                : bytes == null ? 0L : Long.parseLong(bytes.toString());
        return ImageUploadResult.builder()
                .url((String) uploadResult.get("secure_url"))
                .publicId((String) uploadResult.get("public_id"))
                .size(size)
                .format((String) uploadResult.get("format"))
                .build();
    }
}