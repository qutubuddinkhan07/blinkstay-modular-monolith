package com.blinkstay.support.builders;

import com.blinkstay.booking.entities.Booking;
import com.blinkstay.booking.enums.BookingStatus;
import com.blinkstay.support.TestDates;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds a valid Booking with sensible defaults, so a test only states what matters:
 * <p>
 * Booking b = aBooking().confirmed().checkIn(today().plusDays(5)).nights(2).build();
 * <p>
 * Defaults: CONFIRMED, check-in 10 days from today, 2 nights, 1 room, 2 guests,
 * 2000.00 INR per night, random ids. id stays null (JPA sets it) unless withId() is used.
 */
public final class BookingBuilder {
    private UUID id;
    private String reference = randomReference();
    private UUID guestId = UUID.randomUUID();
    private UUID hostId = UUID.randomUUID();
    private UUID listingId = UUID.randomUUID();
    private UUID roomId = UUID.randomUUID();
    private LocalDate checkIn = TestDates.today().plusDays(10);
    private int nights = 2;
    private int rooms = 1;
    int guests = 2;
    private BookingStatus status = BookingStatus.CONFIRMED;
    private BigDecimal pricePerNight = new BigDecimal("2000.00");
    private BigDecimal totalAmount; // null = pricePerNight x nights x rooms
    private Instant holdExpiresAt;
    private Instant confirmedAt = Instant.now();
    private UUID paymentId;

    private BookingBuilder() {
    }

    public static BookingBuilder aBooking() {
        return new BookingBuilder();
    }

    // ---- status shortcuts ----

    /**
     * Unpaid, with a hold that is still running for the given time.
     */
    public BookingBuilder pendingPayment(Duration holdLeft) {
        this.status = BookingStatus.PENDING_PAYMENT;
        this.holdExpiresAt = Instant.now().plus(holdLeft);
        this.confirmedAt = null;
        return this;
    }

    /**
     * Unpaid, and the hold ran out the given time ago (not yet relabelled EXPIRED).
     */
    public BookingBuilder pendingPaymentHoldExpired(Duration ago) {
        this.status = BookingStatus.EXPIRED;
        this.holdExpiresAt = Instant.now().minus(ago);
        this.confirmedAt = null;
        return this;
    }

    public BookingBuilder confirmed() {
        this.status = BookingStatus.CONFIRMED;
        this.holdExpiresAt = null;
        this.confirmedAt = Instant.now();
        return this;
    }

    public BookingBuilder status(BookingStatus status) {
        this.status = status;
        return this;
    }

    // ---- stay ----

    public BookingBuilder checkIn(LocalDate checkIn) {
        this.checkIn = checkIn;
        return this;
    }

    public BookingBuilder nights(int nights) {
        this.nights = nights;
        return this;
    }

    /**
     * Sets check-in and check-out together.
     */
    public BookingBuilder stay(LocalDate checkIn, LocalDate checkOut) {
        this.checkIn = checkIn;
        this.nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        return this;
    }

    public BookingBuilder rooms(int rooms) {
        this.rooms = rooms;
        return this;
    }

    public BookingBuilder guests(int guests) {
        this.guests = guests;
        return this;
    }

    // ---- money ----

    public BookingBuilder pricePerNight(String amount) {
        this.pricePerNight = new BigDecimal(amount);
        return this;
    }

    public BookingBuilder totalAmount(String amount) {
        this.totalAmount = new BigDecimal(amount);
        return this;
    }

    // ---- ids ----

    /**
     * Gives the booking an id now, for unit tests where nothing saves it.
     */
    public BookingBuilder withId() {
        this.id = UUID.randomUUID();
        return this;
    }

    public BookingBuilder guest(UUID guestId) {
        this.guestId = guestId;
        return this;
    }

    public BookingBuilder host(UUID hostId) {
        this.hostId = hostId;
        return this;
    }

    public BookingBuilder listing(UUID listingId) {
        this.listingId = listingId;
        return this;
    }

    public BookingBuilder room(UUID roomId) {
        this.roomId = roomId;
        return this;
    }

    public BookingBuilder payment(UUID paymentId) {
        this.paymentId = paymentId;
        return this;
    }

    public Booking build() {
        BigDecimal total = totalAmount != null ? totalAmount
                : pricePerNight.multiply(BigDecimal.valueOf((long) nights * rooms));
        return Booking.builder()
                .id(id)
                .reference(reference)
                .guestId(guestId)
                .hostId(hostId)
                .listingId(listingId)
                .roomId(roomId)
                .checkIn(checkIn)
                .checkOut(checkIn.plusDays(nights))
                .nights(nights)
                .roomsBooked(rooms)
                .guests(guests)
                .status(status)
                .pricePerNight(pricePerNight)
                .totalAmount(total)
                .currency("INR")
                .holdExpiresAt(holdExpiresAt)
                .confirmedAt(confirmedAt)
                .listingTitle("Test listing")
                .listingLocation("Test city")
                .roomType("DOUBLE")
                .paymentId(paymentId)
                .build();
    }

    private static String randomReference() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("T");
        for (int i = 0; i < 9; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt((chars.length()))));
        }
        return sb.toString();
    }
}
