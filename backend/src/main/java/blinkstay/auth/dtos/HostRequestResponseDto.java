package blinkstay.auth.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import blinkstay.auth.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostRequestResponseDto {
	private UUID id;
	private RequestStatus status;
	private String businessName;
	private String address;
	private String description;
	private String website;
	private String rejectionReason;
	private LocalDateTime reviewedAt;
	private LocalDateTime createdAt;
}
