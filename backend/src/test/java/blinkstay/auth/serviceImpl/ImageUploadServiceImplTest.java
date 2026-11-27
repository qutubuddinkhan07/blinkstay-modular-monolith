package blinkstay.auth.serviceImpl;

import blinkstay.auth.dtos.ImageUploadResult;
import blinkstay.common.cloudinary.CloudinaryFolders;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

/**
 * BLINK-18: profile photos are stored exactly as uploaded.
 * These tests fail if any upload sends a crop, resize or quality option to Cloudinary again.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
public class ImageUploadServiceImplTest {

    // every key that would change the stored image on upload
    private static final String[] IMAGE_CHANGING_KEYS = {"transformation", "transformations", "width", "height",
            "crop", "gravity", "quality"};

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private ImageUploadServiceImpl service;

    @BeforeEach
    void setup() {
        // the real folder class with the dev prefix: no String needed
        service = new ImageUploadServiceImpl(cloudinary, new CloudinaryFolders("dev"));
    }

    // Cloudinary's answer for a successful upload; only stubbed in tests that reach the upload
    private void cloudinaryAcceptsUpload() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/v1/dev/blinkstay_user_profiles/u1.jpg",
                "public_id", "dev/blinkstay_user_profiles/u1",
                "bytes", 2_345_678,
                "format", "jpg"
        ));
    }

    private Map<String, Object> sentUploadOptions() throws Exception {
        ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(), options.capture());
        return options.getValue();
    }

    private static MockMultipartFile photo(String filename) {
        return new MockMultipartFile("profileImg", filename, "image/jpeg", new byte[]{1, 2, 3, 4});
    }

    @Test
    @DisplayName("new profile photo is uploaded as the original: no crop, resize or quality option")
    void uploadImage_sendNoTransformation_andUsesProfileFolder() throws Exception {
        cloudinaryAcceptsUpload();

        ImageUploadResult result = service.uploadImage(photo("me.jpg"), "u1");

        Map<String, Object> sent = sentUploadOptions();
        assertThat(sent).doesNotContainKeys(IMAGE_CHANGING_KEYS);
        assertThat(sent).containsEntry("folder", "dev/blinkstay_user_profiles").containsEntry("public_id", "u1");
        assertThat(result.getUrl()).endsWith("/u1.jpg");
        assertThat(result.getSize()).isEqualTo(2_345_678L);
    }

    @Test
    @DisplayName("changing the profile photo overwrites the same id, refreshes the CDN, and sends no crop")
    void updateImage_overwritesAndInvalidates_withoutTransformation() throws Exception {
        cloudinaryAcceptsUpload();

        service.updateImage(photo("new.png"), "u1");

        Map<String, Object> sent = sentUploadOptions();
        assertThat(sent).doesNotContainKeys(IMAGE_CHANGING_KEYS);
        assertThat(sent).containsEntry("public_id", "u1")
                .containsEntry("overwrite", true)
                .containsEntry("invalidate", true)
                .containsEntry("folder", "dev/blinkstay_user_profiles");
    }

    @Test
    @DisplayName("upload from bytes (used by account flows) also sends no crop")
    void uploadImageFromBytes_sendsNoTransformation() throws Exception {
        cloudinaryAcceptsUpload();

        service.uploadImage(new byte[]{9, 9, 9}, "ui");
        assertThat(sentUploadOptions()).doesNotContainKeys(IMAGE_CHANGING_KEYS);
    }

    @Test
    @DisplayName("an empty file is refused with 400 before anything is sent to Cloudinary")
    void uploadImage_emptyFail_isBadRequest_andNothingUploaded() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("profileImg", "me.jpn", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.uploadImage(empty, "u1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("a file that is not jpg, jpeg, png or gif is refused with 400 before upload")
    void updateImage_wrongExtension_isBadRequest_andNothingUploaded() {
        assertThatThrownBy(() -> service.updateImage(photo("me.webp"), "u1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(cloudinary);
    }
}
