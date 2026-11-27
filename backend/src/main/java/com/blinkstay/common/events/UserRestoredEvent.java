package com.blinkstay.common.events;

import java.util.UUID;

/**
 * stillBlocked = the account was blocked before it was deleted, so it stays blocked.
 * dataPurged = the 30-day purge had already run, so the image, reviews and listings are gone.
 */
public record UserRestoredEvent(UUID userId, boolean stillBlocked, boolean dataPurged) {
}