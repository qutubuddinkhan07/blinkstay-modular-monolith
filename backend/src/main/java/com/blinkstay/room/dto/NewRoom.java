package com.blinkstay.room.dto;

import java.math.BigDecimal;

import com.blinkstay.room.enums.RoomCategory;

public record NewRoom(RoomCategory roomType, BigDecimal price, Integer totalRooms, Integer maxGuests) {
}
