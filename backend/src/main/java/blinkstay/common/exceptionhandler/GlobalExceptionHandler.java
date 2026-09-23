package blinkstay.common.exceptionhandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import blinkstay.auth.dtos.ApiResponse;
import blinkstay.common.exception.ManagerNotOwnerException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(IllegalArgumentException ex) {
		ApiResponse<String> apiResponse = ApiResponse.<String>builder().success(false).message(ex.getMessage())
				.data(null).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
	}

	@ExceptionHandler({ MethodArgumentNotValidException.class, HandlerMethodValidationException.class })
	public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(Exception ex) {

		Map<String, String> errors = new HashMap<>();

		if (ex instanceof MethodArgumentNotValidException methodEx) {

			methodEx.getBindingResult().getFieldErrors()
					.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

		} else if (ex instanceof HandlerMethodValidationException handlerEx) {

			handlerEx.getAllErrors().forEach(error -> {

				if (error instanceof FieldError fieldError) {

					errors.put(fieldError.getField(), fieldError.getDefaultMessage());

				} else {

					errors.put("validationError", error.getDefaultMessage());
				}
			});
		}

		ApiResponse<Map<String, String>> apiResponse = ApiResponse.<Map<String, String>>builder().success(false)
				.message("Validation failed").data(errors).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
	}

	@ExceptionHandler({ ResponseStatusException.class, AuthorizationDeniedException.class,
			ManagerNotOwnerException.class })
	public ResponseEntity<ApiResponse<String>> handleAuthorizationExceptions(Exception ex) {

		HttpStatusCode status;
		String message;

		if (ex instanceof ResponseStatusException rse) {

			status = rse.getStatusCode();
			message = rse.getReason();

		} else if (ex instanceof ManagerNotOwnerException managerEx) {

			status = HttpStatus.FORBIDDEN;
			message = managerEx.getMessage();

		} else {

			// AuthorizationDeniedException
			status = HttpStatus.FORBIDDEN;
			message = "Access denied";
		}

		ApiResponse<String> apiResponse = ApiResponse.<String>builder().success(false).message(message).data(null)
				.build();

		return ResponseEntity.status(status).body(apiResponse);
	}

	// This is for handling the errors occurring while providing invalid "ENUM"
	// values
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<String>> handleHttpMessageNotReadableExceptino(
			HttpMessageNotReadableException ex) {
		String errorMessage = "Malformed JSON request or invalid input format.";

		// Check if the cause is an invalid Enum/target format mismatch
		if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {
			Class<?> targetType = invalidFormatException.getTargetType();

			if (targetType != null && targetType.isEnum()) {
				Object value = invalidFormatException.getValue();
				Object[] enumCOnstants = targetType.getEnumConstants();

				errorMessage = String.format("Invalid value '%s' for field '%s'. Accepted values are: %s", value,
						invalidFormatException.getPath().isEmpty() ? "field"
								: invalidFormatException.getPath().get(0).getFieldName(),
						java.util.Arrays.toString(enumCOnstants));
			}
		}

		ApiResponse<String> apiResponse = ApiResponse.<String>builder().success(false).message(errorMessage).data(null)
				.build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
	}

	// This is used to track the file not provided exception
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<List<String>>> handleConstraintViolationException(
			ConstraintViolationException ex) {
		List<String> errors = ex.getConstraintViolations().stream()
				.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage()).toList();

		ApiResponse<List<String>> apiResponse = ApiResponse.<List<String>>builder().success(false)
				.message("Parameter validation failed").data(errors).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
	}

	// for handling image related errors

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ApiResponse<String>> handleMissingServletRequestPartException(
			MissingServletRequestPartException ex) {
		String errorMessage = String.format("Required request part '%s' is missing from the multipart request.",
				ex.getRequestPartName());

		ApiResponse<String> apiResponse = ApiResponse.<String>builder().success(false).message(errorMessage).data(null)
				.build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
	}

	@ExceptionHandler(NullPointerException.class)
	public ResponseEntity<ApiResponse<String>> handleNullPointerException(NullPointerException ex) {

		ApiResponse<String> apiResponse = ApiResponse.<String>builder().success(false)
				.message("A required value was missing or null: " + ex.getMessage()).data(null).build();

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<String>> handleGeneralException(Exception ex) {

		ApiResponse<String> apiResponse = ApiResponse.<String>builder().success(false)
				.message("An unexpected error occurred").data(null).build();

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
	}
}