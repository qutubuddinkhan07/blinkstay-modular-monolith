package com.blinkstay.common.image;

import java.util.Arrays;
import java.util.Locale;

/**
 * Where an image goes, and which file types that place accepts.
 * Both accept AVIF. Profile photos may also be animated GIFs; listing photos may not.
 */
public enum ImageFolder {
    PROFILE("jpg", "jpeg", "png", "gif", "avif"),
    LISTING("jpg", "jpeg", "png", "avif");

    private final String[] formats;

    ImageFolder(String... formats) {
        this.formats = formats;
    }

    public String[] formats() {
        return formats.clone();
    }

    public boolean allows(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }

        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return Arrays.asList(formats).contains(ext);
    }

    public String allowedList() {
        return String.join(", ", formats);
    }
}
