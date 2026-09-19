package blinkstay.auth.exceptionhandler;

import blinkstay.auth.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(Exception ex) {
        Map<String, String> errors = new HashMap<>();

        if (ex instanceof MethodArgumentNotValidException methodEx) {
            methodEx.getBindingResult().getFieldErrors().forEach(
                    error -> errors.put(error.getField(), error.getDefaultMessage())
            );
        } else if (ex instanceof HandlerMethodValidationException handlerEx) {
            handlerEx.getAllErrors().forEach(
                    error -> {
                        if (error instanceof FieldError fieldError) {
                            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
                        } else {
                            errors.put("validationError: ", error.getDefaultMessage());
                        }
                    }
            );
        }

        ApiResponse<Map<String, String>> apiResponse = ApiResponse
                .<Map<String, String>>builder()
                .success(false)
                .message("Method Argument Exception")
                .data(errors).build();

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<String>> handleNullPointerException(NullPointerException ex) {
        ApiResponse<String> apiResponse =
                ApiResponse.<String>builder()
                        .success(false)
                        .message("A required value was missing or null: " + ex.getMessage())
                        .data(null)
                        .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGeneralException(Exception ex) {
        ApiResponse<String> apiResponse = ApiResponse.<String>
                        builder().success(false).message("An unexpected error occured: " + ex.getMessage())
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }
}
