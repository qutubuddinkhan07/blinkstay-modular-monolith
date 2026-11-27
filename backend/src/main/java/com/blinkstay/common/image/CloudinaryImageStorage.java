package com.blinkstay.common.image;

import com.blinkstay.common.cloudinary.CloudinaryFolders;
import com.cloudinary.Cloudinary;
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
 * Stores images on Cloudinary exactly as uploaded. Never add transformation, width,
 * height, crop, gravity or quality to the upload options: CloudinaryImageStorageTest fails if you do.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"unchecked", "rawtypes"})
public class CloudinaryImageStorage implements ImageStorage {

    private final Cloudinary cloudinary;
    private final CloudinaryFolders folders;

    @Override
    public ImageUploadResult upload(MultipartFile file, ImageFolder folder, String publicId) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload a valid image file.");
        }
        if (!folder.allows(file.getOriginalFilename())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid image format. Allowed formats are: " + folder.allowedList() + ".");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file.");
        }
        return send(bytes, folder, publicId);
    }

    @Override
    public ImageUploadResult upload(byte[] bytes, ImageFolder folder, String publicId) {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload a valid image file.");
        }
        return send(bytes, folder, publicId);
    }

    private ImageUploadResult send(byte[] bytes, ImageFolder folder, String publicId) {
        Map<String, Object> options = new HashMap<>();
        options.put("folder", folderName(folder));
        options.put("allowed_formats", folder.formats()); // Cloudinary checks the real content too
        if (publicId != null && !publicId.isBlank()) {
            options.put("public_id", publicId);
        }
        try {
            Map result = cloudinary.uploader().upload(bytes, options);
            return toResult(result);
        } catch (Exception e) {
            log.error("Cloudinary upload to {} failed", folder, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Image upload failed. Please try a different image.");
        }
    }

    @Override
    public boolean delete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("Public ID is required for deletion");
        }
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String outcome = String.valueOf(result.get("result"));
            return "ok".equals(outcome) || "not found".equals(outcome); // either way, the file is gone
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete image " + publicId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> details(String publicId) {
        try {
            return cloudinary.api().resource(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get image details: " + e.getMessage(), e);
        }
    }

    private String folderName(ImageFolder folder) {
        return switch (folder) {
            case PROFILE -> folders.userProfiles();
            case LISTING -> folders.listings();
        };
    }

    // Cloudinary returns the size as Integer or Long depending on the file
    private static ImageUploadResult toResult(Map result) {
        Object bytes = result.get("bytes");
        long size = bytes instanceof Number n ? n.longValue()
                : bytes == null ? 0L : Long.parseLong(bytes.toString());
        return new ImageUploadResult((String) result.get("secure_url"), (String) result.get("public_id"),
                size, (String) result.get("format"));
    }
}