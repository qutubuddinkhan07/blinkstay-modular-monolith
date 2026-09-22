package blinkstay.listing.service;

import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import blinkstay.listing.dto.ImageUploadResult;

public interface ImageUploadService {
	ImageUploadResult uploadImage(MultipartFile file, String customPublicId);

	ImageUploadResult uploadImage(byte[] imageBytes, String publicId);

	Map<String, Object> deleteImage(String publicId);

	ImageUploadResult replaceImage(MultipartFile newFile, String oldPublicId);

	ImageUploadResult updateImage(MultipartFile newFile, String publicId);

	Map<String, Object> getImageDetails(String pulicId);
}
