package blinkstay.listing.serviceImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;

import blinkstay.listing.dtos.ImageUploadResult;
import blinkstay.listing.mapper.ModelMapper;
import blinkstay.listing.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("lisitingImageUploadService")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ImageUploadServiceImpl implements ImageUploadService {
	private final Cloudinary cloudinary;

	private final ModelMapper modelMapper;

	@Override
	public ImageUploadResult uploadImage(MultipartFile file, String customPublicId) {
		try {
			if (file.isEmpty()) {
				throw new RuntimeException("File is empty");
			}

			Map<String, Object> uploadParams = new HashMap<>();
			uploadParams.put("folder", "blinkstay_listings");
			uploadParams.put("allowed_formats", new String[] { "jpg", "png", "jpeg", "gif" });

			if (customPublicId != null && !customPublicId.isEmpty()) {
				uploadParams.put("public_id", customPublicId);
			}

			uploadParams.put("transformations",
					new Transformation<>().width(500).height(500).crop("fill").gravity("center"));

			Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

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

			ImageUploadResult imageUploadResult = modelMapper.objectToImageUploadResult(uploadResult, size);
			return imageUploadResult;
		} catch (IOException e) {
			throw new RuntimeException("Failed to upload Image: " + e.getMessage());
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

			ImageUploadResult imageUploadResult = modelMapper.objectToImageUploadResult(uploadResult, size);

			return imageUploadResult;
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
			uploadParams.put("allowed_formats", new String[] { "jpg", "jpeg", "png", "gif" });

			Map uploadResult = cloudinary.uploader().upload(newFile.getBytes(), uploadParams);

			Object bytesObj = uploadResult.get("bytes");
			Long size = 0L;
			if (bytesObj instanceof Long) {
				size = (Long) bytesObj;
			} else if (bytesObj instanceof Integer) {
				size = ((Integer) bytesObj).longValue();
			} else if (bytesObj != null) {
				size = Long.parseLong(bytesObj.toString());
			}

			return modelMapper.objectToImageUploadResult(uploadResult, size);
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

}
