package com.blinkstay.common.image;

/**
 * What Cloudinary stored: the public URL, its id (needed to delete it), size in bytes and format.
 */
public record ImageUploadResult(String url, String publicId, long size, String format) {
}
