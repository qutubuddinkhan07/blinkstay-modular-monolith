package blinkstay.room.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import blinkstay.listing.service.ListingService;
import blinkstay.room.dto.AddRoomDto;
import blinkstay.room.dto.RoomApiResponse;
import blinkstay.room.service.RoomService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v4/rooms")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAuthority('HOTEL_MANAGER')")
public class RoomController {

	private final RoomService roomService;
	private final ListingService listingService;

	@PostMapping("/{listingId}/rooms")
	public ResponseEntity<RoomApiResponse<String>> createRoom(@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable UUID listingId, @Valid @RequestBody AddRoomDto addRoomDto) {

		UUID userId = UUID.fromString(userDetails.getUsername());

		// Authorization check
		listingService.checkWhetherSameManager(userId, listingId);

		// Room operation
		String response = roomService.createRoom(listingId, addRoomDto);

		RoomApiResponse<String> apiResponse = RoomApiResponse.<String>builder().success(true).message("Room created")
				.data(response).build();

		return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
	}

	@PutMapping("/{listingId}/rooms/{roomId}")
	public ResponseEntity<RoomApiResponse<String>> createRoom(@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable UUID listingId, @PathVariable UUID roomId, @Valid @RequestBody AddRoomDto addRoomDto) {

		UUID userId = UUID.fromString(userDetails.getUsername());

		// Authorization check
		listingService.checkWhetherSameManager(userId, listingId);

		// Room operation
		String response = roomService.updateRoom(listingId, roomId, addRoomDto);

		RoomApiResponse<String> apiResponse = RoomApiResponse.<String>builder().success(true).message("Room updated")
				.data(response).build();

		return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
	}

	public ResponseEntity<RoomApiResponse<String>> deleteRoomById(@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable("listingId") UUID listingId, @PathVariable("roomId") UUID roomId) {
		UUID userId = UUID.fromString(userDetails.getUsername());

		// Authorization check
		listingService.checkWhetherSameManager(userId, listingId);

		// Room operation
		String response = roomService.deleteRoom(roomId);

		RoomApiResponse<String> apiResponse = RoomApiResponse.<String>builder().success(true).message("Room updated")
				.data(response).build();

		return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
	}

}
