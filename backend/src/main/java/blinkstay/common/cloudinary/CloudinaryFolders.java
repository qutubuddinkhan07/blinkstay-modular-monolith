package blinkstay.common.cloudinary;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * One place for Cloudinary folder names. Each environment gets its own prefix
 * (dev/..., prod/...), so a job in one environment can never see the other's images.
 */
@Component
public class CloudinaryFolders {
    private final String prefix;

    public CloudinaryFolders(@Value("${app.cloudinary.folder-prefix:dev}") String prefix) {
        String p = prefix == null ? "" : prefix.trim().replaceAll("/+$", "");
        this.prefix = p.isEmpty() ? "" : p + "/";
    }

    public String listings() {
        return prefix + "blinkstay_listings";
    }

    public String userProfiles() {
        return prefix + "blinkstay_user_profiles";
    }
}