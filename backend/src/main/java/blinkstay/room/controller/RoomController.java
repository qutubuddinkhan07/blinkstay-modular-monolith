package blinkstay.room.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import blinkstay.room.dto.AddRoomDto;
import blinkstay.room.dto.RoomApiResponse;
import blinkstay.room.service.RoomService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v4/rooms")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAuthority('HOTEL_MANAGER')")
@RequiredArgsConstructor
public class RoomController {
	private final RoomService roomService;

	@PostMapping("/{listingId}/rooms")
	public ResponseEntity<RoomApiResponse<String>> createRoom(@AuthenticationPrincipal UserDetails userDetails,
			@Valid @PathVariable("listingId") UUID listingId, @RequestBody AddRoomDto addRoomDto) {

		UUID userId = UUID.fromString(userDetails.getUsername());

		roomService.checkIfSameManger(userId, listingId);

		String response = roomService.createRoom(listingId, addRoomDto);

		RoomApiResponse<String> apiResponse = RoomApiResponse.<String>builder().success(true).message("Room created")
				.data(response).build();

		return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
	}
}
