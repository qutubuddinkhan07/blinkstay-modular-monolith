package blinkstay.common.exceptionhandler;

import blinkstay.auth.dtos.ApiResponse;
import blinkstay.common.exception.AccountBlockedException;
import blinkstay.common.exception.ManagerNotOwnerException;
import blinkstay.common.exception.UserAlreadyExistsException;
import blinkstay.common.exception.UserNotFoundException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private static final String GENERIC = "Something went wrong on our side. Please try again.";

    private static ResponseEntity<ApiResponse<String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.<String>builder().success(false).message(message).data(null).build());
    }

    private static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status)
                .body(ApiResponse.<T>builder().success(false).message(message).data(data).build());
    }

    /**
     * Handles missing required HTTP request parameters (@RequestParam).
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<String>> handleMissingParam(MissingServletRequestParameterException ex) {
        return error(HttpStatus.BAD_REQUEST, "Missing required parameter '" + ex.getParameterName() + "'");
    }

    /**
     * Handles requests with an unsupported Content-Type media header.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported content type");
    }

    /**
     * Handles uploaded files exceeding the configured maximum upload size limit.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<String>> handleTooLarge(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.CONTENT_TOO_LARGE, "The file is too large (maximum 10 MB)");
    }

    /**
     * Handles database constraint/integrity violations (e.g., unique index or foreign key violations).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return error(HttpStatus.CONFLICT, "This change conflicts with existing data");
    }

    /**
     * Fallback handler for unhandled NullPointerExceptions.
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<String>> handleNullPointerException(NullPointerException ex) {
        log.error("Null pointer", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC);
    }

    /**
     * Fallback handler for general RuntimeExceptions not caught elsewhere.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(RuntimeException ex) {
        log.error("Unhandled runtime exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC);
    }

    /**
     * Global catch-all fallback handler for any remaining unhandled Exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC);
    }

    /**
     * Handles invalid argument exceptions thrown across services and controllers.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles validation errors from @Valid body payloads or method-level parameter validations, returning a map of field errors.
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class})
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

        return error(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    /**
     * Handles blocked user account attempts, returning a specific status string in data.
     */
    @ExceptionHandler(AccountBlockedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccountBlocked(AccountBlockedException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage(), "ACCOUNT_BLOCKED");
    }

    /**
     * Handles scenarios where a requested user entity cannot be found.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleUserNotFound(UserNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles conflicts when attempting to create a user that already exists.
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<String>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles dynamic response status exceptions, authorization denials, or mismatched resource ownerships.
     */
    @ExceptionHandler({ResponseStatusException.class, AuthorizationDeniedException.class,
            ManagerNotOwnerException.class})
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
            status = HttpStatus.FORBIDDEN;
            message = "Access denied";
        }

        return error(HttpStatus.valueOf(status.value()), message);
    }

    /**
     * Handles errors occurring while parsing request body JSON, specifically targeting invalid ENUM values and mismatch types.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        String errorMessage = "Malformed JSON request or invalid input format.";

        if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {
            Class<?> targetType = invalidFormatException.getTargetType();

            if (targetType != null && targetType.isEnum()) {
                Object value = invalidFormatException.getValue();
                Object[] enumConstants = targetType.getEnumConstants();

                errorMessage = String.format("Invalid value '%s' for field '%s'. Accepted values are: %s", value,
                        invalidFormatException.getPath().isEmpty() ? "field"
                                : invalidFormatException.getPath().get(0).getFieldName(),
                        Arrays.toString(enumConstants));
            }
        }

        return error(HttpStatus.BAD_REQUEST, errorMessage);
    }

    /**
     * Tracks and handles request constraint violations (e.g. @RequestParam / @PathVariable validation annotations).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage()).toList();

        return error(HttpStatus.BAD_REQUEST, "Parameter validation failed", errors);
    }

    /**
     * Handles missing file parts in multipart form-data requests.
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<String>> handleMissingServletRequestPartException(
            MissingServletRequestPartException ex) {
        String errorMessage = String.format("Required request part '%s' is missing from the multipart request.",
                ex.getRequestPartName());

        return error(HttpStatus.BAD_REQUEST, errorMessage);
    }

    /**
     * Handles standard Apache Coyote / Spring Bad Request exceptions.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<String>> handleBadRequestException(BadRequestException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles method argument type mismatch issues (e.g., passing a string where a Long ID is expected).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadId(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value for '" + ex.getName() + "'", null);
    }

    /**
     * Handles optimistic locking failures when a record has been modified concurrently by another transaction.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<String>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return error(HttpStatus.CONFLICT, "This record was changed by someone else. Please try reload and try again.");
    }

    /**
     * Handles requests targeting URLs/endpoints that do not exist (No handler found).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNoResource(NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "Resource not found");
    }

    /**
     * Handles requests where the URL exists but the HTTP request method is unsupported (e.g., GET on a POST endpoint).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed for this endpoint");
    }
}