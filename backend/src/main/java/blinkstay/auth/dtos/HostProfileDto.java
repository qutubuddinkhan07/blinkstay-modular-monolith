package blinkstay.auth.dtos;

import java.time.LocalDateTime;

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
public class HostProfileDto {
	private String businessName;
	private String address;
	private String description;
	private String website;
	private LocalDateTime approvedAt;
	private LocalDateTime updatedAt;
}
