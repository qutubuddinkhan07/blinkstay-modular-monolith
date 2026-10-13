package blinkstay.auth.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import blinkstay.auth.util.TrimmedStringDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AddUserDto {
	@NotBlank(message = "username can't be empty")
	@JsonDeserialize(using = TrimmedStringDeserializer.class)
	private String username;

	@NotBlank(message = "email can't be empty")
	@Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "Invalid email format")
	@JsonDeserialize(using = TrimmedStringDeserializer.class)
	private String email;

	@NotBlank(message = "password can't be empty")
	@JsonDeserialize(using = TrimmedStringDeserializer.class)
	private String password;
}

/*-
Email verification
Matches:
    john.doe@example.com
    user+tag@sub.domain.org
    ADMIN_123@domain.co
Does NOT match:
    john.doe@example (Fails: TLD missing)
    john@domain.something (Fails: .something exceeds the 6-character TLD limit)
    john doe@example.com (Fails: Spaces are not allowed in the local part)
    @example.com (Fails: Local part requires at least 1 character)
 */