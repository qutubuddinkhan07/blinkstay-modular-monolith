package com.blinkstay.listing.service;

import java.util.UUID;

import com.blinkstay.listing.serviceImpl.ListingImportCommand;

public interface ListingImportService {
	boolean hasListings();

	UUID importListing(ListingImportCommand command);
}
