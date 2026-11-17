package blinkstay.listing.serviceImpl;

import blinkstay.listing.dto.ImageUploadResult;
import blinkstay.listing.mapper.ModelMapper;
import blinkstay.listing.service.ImageUploadService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Service("lisitingImageUploadService")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"unchecked", "rawtypes"})
public class ImageUploadServiceImpl implements ImageUploadService {
    private final Cloudinary cloudinary;

    private final ModelMapper modelMapper;

    @Override
    public ImageUploadResult uploadImage(MultipartFile file, String customPublicId) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        try {
            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("folder", "blinkstay_listings");
            uploadParams.put("allowed_formats", new String[]{"jpg", "png", "jpeg", "gif", "avif"});

            if (customPublicId != null && !customPublicId.isEmpty()) {
                uploadParams.put("public_id", customPublicId);
            }

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            return modelMapper.objectToImageUploadResult(uploadResult, sizeOf(uploadResult.get("bytes")));
        } catch (Exception e) {
            log.error("Listing image upload failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Image upload failed. Use a jpg, png, jpeg, gif or avif image and try again.");
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

    // Cloudinary returns the size as Integer or Long depending on the file
    private static long sizeOf(Object bytes) {
        if (bytes instanceof Number n) {
            return n.longValue();
        }
        return bytes == null ? 0L : Long.parseLong(bytes.toString());
    }
}