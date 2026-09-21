package blinkstay.auth.service;

import org.springframework.web.multipart.MultipartFile;

import blinkstay.auth.dtos.ImageUploadResult;

import java.util.Map;

public interface ImageUploadService {
    ImageUploadResult uploadImage(MultipartFile file, String customPublicId);

    ImageUploadResult uploadImage(byte[] imageBytes, String publicId);

    // DELETE - by public_id
    Map<String, Object> deleteImage(String publicId);

    // REPLACE/UPDATE - delete old and upload new (using same public_id)
    ImageUploadResult replaceImage(MultipartFile newFile, String oldPublicId);

    // UPDATE - using overwrite parameter (more efficient than delete+upload)
    ImageUploadResult updateImage(MultipartFile newFile, String publicId);

    // GET - image details by public_id
    Map<String, Object> getImageDetails(String publicId);

    // GENERATE - transformed URL without re-uploading
    String getTransformedUrl(String publicId, int width, int height);
}
