package blinkstay.room.entities;

import java.math.BigDecimal;
import java.util.UUID;

import blinkstay.room.enums.RoomCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "listing_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ListingRoom {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private UUID listingId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RoomCategory roomType;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private Integer totalRooms;

	@Column(nullable = false)
	private Integer availableRooms;
}
