package blinkstay.common.exception;

@SuppressWarnings("serial")
public class ManagerNotOwnerException extends RuntimeException {
	public ManagerNotOwnerException(String message) {
		super(message);
	}
}
