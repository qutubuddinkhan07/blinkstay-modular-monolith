package blinkstay.listing.constants;

import blinkstay.listing.entities.ListingImage;

public final class ImagePublicIds {

	public static final String SEED_PREFIX = "seed_";
	public static final String SEEDING_FOLDER = "blinkstay_seeding_listings";

	private ImagePublicIds() {
	}

	/**
	 * True for images created by the seeder (shared files that must never be
	 * deleted).
	 */
	public static boolean isSeed(ListingImage image) {
		String publicId = image.getPublicId();
		String url = image.getImageUrl();

		return (publicId != null && publicId.startsWith(SEED_PREFIX))
				|| (url != null && url.contains("/" + SEEDING_FOLDER + "/"));
	}
}