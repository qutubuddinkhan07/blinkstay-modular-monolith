package blinkstay.booking.dto;

import java.util.List;
import java.util.UUID;

public record RoomCalendarDto(UUID roomId, String roomType, int totalRooms, List<CalendarDayDto> days) {
}