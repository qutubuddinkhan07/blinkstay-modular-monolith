package blinkstay.support.builders;

import blinkstay.room.entities.ListingRoom;
import blinkstay.room.enums.RoomCategory;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Builds a valid ListingRoom (a room TYPE with a quantity):
 * <p>
 * ListingRoom room = aRoom().totalRooms(3).build();
 * <p>
 * Defaults: first RoomCategory value, 2000.00 per night, 3 rooms, 2 guests each.
 */
public final class RoomBuilder {

    private UUID id;
    private UUID listingId = UUID.randomUUID();
    private RoomCategory type = RoomCategory.values()[0];
    private BigDecimal price = new BigDecimal("2000.00");
    private int totalRooms = 3;
    private int maxGuests = 2;

    private RoomBuilder() {
    }

    public static RoomBuilder aRoom() {
        return new RoomBuilder();
    }

    public RoomBuilder withId() {
        this.id = UUID.randomUUID();
        return this;
    }

    public RoomBuilder listing(UUID listingId) {
        this.listingId = listingId;
        return this;
    }

    public RoomBuilder type(RoomCategory type) {
        this.type = type;
        return this;
    }

    public RoomBuilder price(String price) {
        this.price = new BigDecimal(price);
        return this;
    }

    public RoomBuilder totalRooms(int totalRooms) {
        this.totalRooms = totalRooms;
        return this;
    }

    public RoomBuilder maxGuests(int maxGuests) {
        this.maxGuests = maxGuests;
        return this;
    }

    public ListingRoom build() {
        return ListingRoom.builder()
                .id(id)
                .listingId(listingId)
                .roomType(type)
                .price(price)
                .totalRooms(totalRooms)
                .maxGuests(maxGuests)
                .build();
    }
}