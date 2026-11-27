package com.blinkstay.common.image;

import com.blinkstay.common.cloudinary.CloudinaryFolders;
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
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

/**
 * BLINK-37: one image service for the whole app.
 * Includes BLINK-18's rule: nothing that changes the stored image is ever sent to Cloudinary.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
public class CloudinaryImageStorageTest {

    // every option that would change the store image on upload (BLINK-18)
    private static final String[] IMAGE_CHANGING_KEYS = {"transformation", "transformations", "width", "height", "crop"
            , "gravity", "quality"};

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryImageStorage storage;

    @BeforeEach
    void setup() {
        storage = new CloudinaryImageStorage(cloudinary, new CloudinaryFolders("dev"));
    }

    // only stubbed in tests that really reach Cloudinary (strict stubs fail on unused ones)
    private void cloudinaryAcceptsUpload() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/v1/dev/x/u1.jpg",
                "public_id", "dev/x/u1",
                "bytes", 2_345_678,
                "format", "jpg"
        ));
    }

    private Map<String, Object> sentOptions() throws Exception {
        ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(), options.capture());
        return options.getValue();
    }

    private static MockMultipartFile file(String filename) {
        return new MockMultipartFile("image", filename, "image/jpeg", new byte[]{1, 2, 3, 4});
    }

    private static void assertStatus(Throwable e, HttpStatus status) {
        assertThat(e).isInstanceOf(ResponseStatusException.class);
        HttpStatusCode code = ((ResponseStatusException) e).getStatusCode();
        assertThat(code).isEqualTo(status);
    }

    // ---- uploads: where it goes, and nothing that changes the image ----
    @Test
    @DisplayName("profile photo goes to the profile folder as the original, with the profile formats")
    void upload_profile_storesOriginalInProfileFolder() throws Exception {
        cloudinaryAcceptsUpload();

        ImageUploadResult result = storage.upload(file("me.jpg"), ImageFolder.PROFILE, "u1");

        Map<String, Object> sent = sentOptions();
        assertThat(sent).doesNotContainKeys(IMAGE_CHANGING_KEYS);
        assertThat(sent).containsEntry("folder", "dev/blinkstay_user_profiles")
                .containsEntry("public_id", "u1");
        assertThat((String[]) sent.get("allowed_formats")).containsExactlyInAnyOrder("jpg", "jpeg", "png", "gif", "avif");
        assertThat(result.url()).endsWith("/u1.jpg");
        assertThat(result.publicId()).isEqualTo("dev/x/u1");
        assertThat(result.size()).isEqualTo(2_345_678L);
        assertThat(result.format()).isEqualTo("jpg");
    }

    @Test
    @DisplayName("listing photo goes to the listing folder as the original; listing format allow avif, not gif")
    void upload_listing_storesOriginalInListingFolder() throws Exception {
        cloudinaryAcceptsUpload();

        storage.upload(file("room.avif"), ImageFolder.LISTING, "l1");

        Map<String, Object> sent = sentOptions();
        assertThat(sent).doesNotContainKeys(IMAGE_CHANGING_KEYS);
        assertThat(sent).containsEntry("folder", "dev/blinkstay_listings");
        assertThat((String[]) sent.get("allowed_formats")).containsExactlyInAnyOrder("jpg", "jpeg", "png", "avif");
    }

    @Test
    @DisplayName("registration upload from bytes also stores the original in the profile folder")
    void uploadBytes_profile_storesOriginal() throws Exception {
        cloudinaryAcceptsUpload();

        storage.upload(new byte[]{9, 9, 9}, ImageFolder.PROFILE, "user_1");

        Map<String, Object> sent = sentOptions();
        assertThat(sent).doesNotContainKeys(IMAGE_CHANGING_KEYS);
        assertThat(sent).containsEntry("folder", "dev/blinkstay_user_profiles")
                .containsEntry("public_id", "user_1");
    }

    @Test
    @DisplayName("an animated GIF is accepted as a profile photo")
    void upload_gifProfile_isAccepted() throws Exception {
        cloudinaryAcceptsUpload();

        storage.upload(file("wave.GIF"), ImageFolder.PROFILE, "u1"); // upper case on purpose

        sentOptions(); // reached Cloudinary
    }

    @Test
    @DisplayName("an AVIF is accepted as a profile photo")
    void upload_avifProfile_isAccepted() throws Exception {
        cloudinaryAcceptsUpload();

        storage.upload(file("me.avif"), ImageFolder.PROFILE, "u1");

        sentOptions(); // reached Cloudinary
    }

    // ---- refused before anything is sent ----

    @Test
    @DisplayName("a GIF is refused for a listing with 400, before upload")
    void upload_gifListing_isBadRequest() {
        Throwable e = org.assertj.core.api.Assertions.catchThrowable(
                () -> storage.upload(file("room.gif"), ImageFolder.LISTING, "l1")
        );

        assertStatus(e, HttpStatus.BAD_REQUEST);
        assertThat(e).hasMessageContaining("jpg, jpeg, png, avif");
        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("a type the folder does not accept (webp for a profile) is refused with 400")
    void upload_wrongType_isBadRequest() {
        Throwable e = org.assertj.core.api.Assertions.catchThrowable(
                () -> storage.upload(file("me.webp"), ImageFolder.PROFILE, "u1")
        );

        assertStatus(e, HttpStatus.BAD_REQUEST);
        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("a file name without an extension is refused with 400")
    void upload_noExtension_isBadRequest() {
        Throwable e = org.assertj.core.api.Assertions.catchThrowable(
                () -> storage.upload(file("photo"), ImageFolder.PROFILE, "u1")
        );

        assertStatus(e, HttpStatus.BAD_REQUEST);
        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("an empty file is refused with 400")
    void upload_emptyFile_isBadRequest() {
        MockMultipartFile empty = new MockMultipartFile("image", "me.jpg", "image/jpeg", new byte[0]);

        Throwable e = org.assertj.core.api.Assertions.catchThrowable(
                () -> storage.upload(empty, ImageFolder.PROFILE, "u1")
        );

        assertStatus(e, HttpStatus.BAD_REQUEST);
        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("Cloudinary failing becomes 502, not 500 with internals")
    void upload_cloudinaryFails_isBadGateway() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("timeout"));

        Throwable e = org.assertj.core.api.Assertions.catchThrowable(
                () -> storage.upload(file("me.png"), ImageFolder.PROFILE, "u1")
        );

        assertStatus(e, HttpStatus.BAD_GATEWAY);
        assertThat(e).hasMessageNotContaining("timeout");
    }

    // ---- delete ----

    @Test
    @DisplayName("delete is true when Cloudinary deleted the file")
    void delete_ok_isTrue() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("dev/x/u1"), anyMap())).thenReturn(Map.of("result", "ok"));

        assertThat(storage.delete("dev/x/u1")).isTrue();
    }

    @Test
    @DisplayName("delete is true when the file was already gone")
    void delete_notFound_isTrue() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("dev/x/u1"), anyMap())).thenReturn(Map.of("result", "not found"));

        assertThat(storage.delete("dev/x/u1")).isTrue();
    }

    @Test
    @DisplayName("delete is false when Cloudinary answers anthing else")
    void delete_otherAnswer_isFalse() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("dev/x/u1"), anyMap())).thenReturn(Map.of("result", "error"));

        assertThat(storage.delete("dev/x/u1")).isFalse();
    }

    @Test
    @DisplayName("delete without an id is refused before calling Cloudinary")
    void delete_blankId_isRefused() {
        assertThatThrownBy(() -> storage.delete(" "))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(cloudinary);
    }
}
