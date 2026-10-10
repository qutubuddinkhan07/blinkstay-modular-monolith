package blinkstay.common.exception;

public class AccountBlockedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public AccountBlockedException(String reason) {
		super(reason == null || reason.isBlank() ? "Your account has been blocked."
				: "Your account has been blocked: " + reason);
	}
}
