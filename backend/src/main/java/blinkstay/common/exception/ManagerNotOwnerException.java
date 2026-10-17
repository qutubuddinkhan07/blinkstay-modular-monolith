package blinkstay.common.exception;

public class ManagerNotOwnerException extends RuntimeException {
	public ManagerNotOwnerException(String message) {
		super(message);
	}
}
