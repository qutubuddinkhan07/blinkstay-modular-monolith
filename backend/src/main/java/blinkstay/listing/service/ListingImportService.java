package blinkstay.listing.service;

import java.util.UUID;

import blinkstay.listing.serviceImpl.ListingImportCommand;

public interface ListingImportService {
	boolean hasListings();

	UUID importListing(ListingImportCommand command);
}
