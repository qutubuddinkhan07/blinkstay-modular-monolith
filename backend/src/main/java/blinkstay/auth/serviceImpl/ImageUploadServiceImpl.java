package blinkstay.auth.serviceImpl;

import blinkstay.auth.dto.ImageUploadResult;
import blinkstay.auth.service.ImageUploadService;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadServiceImpl implements ImageUploadService {
    private final Cloudinary cloudinary;

    @Override
    public ImageUploadResult uploadImage(MultipartFile file, String customPublicId) {
        try {
            // Validate file type
            if (file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }

            // Upload to Cloudinary with optional folder structure
            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("folder", "blinkstay_user_profiles");
            uploadParams.put("allowed_formats", new String[]{"jpg", "png", "jpeg", "gif"});

            // Set custom public_id if provided, otherwise Cloudinary generates one
            if (customPublicId != null && !customPublicId.isEmpty()) {
                uploadParams.put("public_id", customPublicId);
            }

            // Optional: Add image transformations during upload
            uploadParams.put("transformation",
                    new Transformation<>().width(500)
                            .height(500)
                            .crop("fill")
                            .gravity("face"));

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            // Type conversion
            Object bytesObj = uploadResult.get("bytes");
            Long size = 0L;
            if (bytesObj instanceof Long) {
                size = (Long) bytesObj;
                log.info("Object bytes[] type: Long");
            } else if (bytesObj instanceof Integer) {
                size = ((Integer) bytesObj).longValue();
                log.info("Object bytes[] type: Integer");
            } else if (bytesObj != null) {
                size = Long.parseLong(bytesObj.toString());
                log.info("Object bytes[] type: String");
            }

            // Return the secure url
            return ImageUploadResult.builder()
                    .url((String) uploadResult.get("secured_url"))
                    .publicId((String) uploadResult.get("public_id"))
                    .size(size)
                    .format((String) uploadResult.get("format"))
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }

    @Override
    public ImageUploadResult uploadImage(byte[] imageBytes, String publicId) {
        try {
            Map<String, Object> uploadParams = new HashMap<>();

            uploadParams.put("folder", "blinkstay_user_profiles");
            uploadParams.put("public_id", publicId);
            Map uploadResult = cloudinary.uploader().upload(imageBytes, uploadParams);

            Object bytesObj = uploadResult.get("bytes");
            long size = 0L;
            if (bytesObj instanceof Long) {
                size = (Long) bytesObj;
            } else if (bytesObj instanceof Integer) {
                size = ((Integer) bytesObj).longValue();
            } else if (bytesObj != null) {
                size = Long.parseLong(bytesObj.toString());
            }

            return ImageUploadResult.builder().url((String) uploadResult.get("secure_url"))
                    .publicId((String) uploadResult.get("public_id")).size(size)
                    .format((String) uploadResult.get("format")).build();
        } catch (IOException e) {
            throw new RuntimeException("Image upload failed");
        }
    }

    // DELETE - by public_id
    @Override
    public Map<String, Object> deleteImage(String publicId) {
        try {
            if (publicId == null || publicId.isEmpty()) {
                throw new RuntimeException("Public ID is required for deletion");
            }

            // Delete from Cloudinary
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

            // Result contains: "result": "ok" or "not found"
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete image: " + e.getMessage());
        }
    }

    // REPLACE/UPDATE - delete old and upload new (using same public_id)
    @Override
    public ImageUploadResult replaceImage(MultipartFile newFile, String oldPublicId) {
        try {
            // First, delete the old image
            if (oldPublicId != null && !oldPublicId.isEmpty()) {
                deleteImage(oldPublicId);
            }

            // Then upload the new image with the SAME public_id
            // This effectively replaces the image at the same URL
            return uploadImage(newFile, oldPublicId);
        } catch (Exception e) {
            throw new RuntimeException("Failed  to replace image: " + e.getMessage());
        }
    }

    @Override
    public ImageUploadResult updateImage(MultipartFile newFile, String publicId) {
        try {
            Map<String, Object> uploadParams = new HashMap<>();

            uploadParams.put("public_id", publicId);
            uploadParams.put("overwrite", true);
            uploadParams.put("folder", "blinkstay_user_profiles");
            uploadParams.put("allowed_formats", new String[]
                    {"jpg", "jpeg", "png", "gif"});

            Map uploadResult = cloudinary.uploader().upload(newFile.getBytes(), uploadParams);

            return ImageUploadResult.builder().url((String) uploadResult.get("secure_url"))
                    .publicId((String) uploadResult.get("public_id")).size((Long) uploadResult.get("bytes"))
                    .format((String) uploadResult.get("format")).build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update image: " + e.getMessage());
        }
    }

    // GET image details by public_id
    @Override
    public Map<String, Object> getImageDetails(String publicId) {
        try {
            Map result = cloudinary.api().resource(publicId, ObjectUtils.emptyMap());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get image details: " + e.getMessage());
        }
    }

    // GENERATE transformed URL without re-uploading
    @Override
    public String getTransformedUrl(String publicId, int width, int height) {
        return cloudinary.url()
                .transformation(new Transformation<>().width(width).height(height)
                        .crop("fill").gravity("face")).generate(publicId);
    }
}
