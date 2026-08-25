package co.istad.ai_interview_app.shared.exception;

import com.google.genai.errors.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class AppGlobalException {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationEx(MethodArgumentNotValidException e) {
        List<FieldErrorResponse> fieldErrorResponseList = new ArrayList<>();
        e.getFieldErrors().forEach(fieldError ->
                fieldErrorResponseList.add(
                        new FieldErrorResponse(
                                fieldError.getField(),
                                fieldError.getDefaultMessage()
                        )
                )
        );
        return ErrorResponse.builder()
                .success(false)
                .status(statusText(HttpStatus.BAD_REQUEST))
                .code(HttpStatus.BAD_REQUEST.value())
                .message("Request data is invalid")
                .timestamp(Instant.now())
                .errors(fieldErrorResponseList)
                .build();
    }

    @ExceptionHandler(value = ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleServiceEx(ResponseStatusException e) {
        HttpStatusCode statusCode = e.getStatusCode();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .status(statusText(statusCode))
                .code(statusCode.value())
                .message(Optional.ofNullable(e.getReason()).orElse(statusText(statusCode)))
                .timestamp(Instant.now())
                .build();

        return new ResponseEntity<>(errorResponse, statusCode);
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> httpMessageNotReadableException(HttpMessageNotReadableException e) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .status(statusText(HttpStatus.BAD_REQUEST))
                .code(HttpStatus.BAD_REQUEST.value())
                .message("Request body is malformed or unreadable")
                .timestamp(Instant.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequestParameter(Exception e) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .status(statusText(HttpStatus.BAD_REQUEST))
                .code(HttpStatus.BAD_REQUEST.value())
                .message("Request parameter is invalid")
                .timestamp(Instant.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityEx(DataIntegrityViolationException e) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .status(statusText(HttpStatus.CONFLICT))
                .code(HttpStatus.CONFLICT.value())
                .message("Request conflicts with existing data")
                .timestamp(Instant.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedEx(AccessDeniedException e) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .status(statusText(HttpStatus.FORBIDDEN))
                .code(HttpStatus.FORBIDDEN.value())
                .message("Access is denied")
                .timestamp(Instant.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * The model provider refused the call — a rejected key, an unknown model, an
     * exhausted quota, an outage on their side.
     *
     * <p>Answered as 503 rather than 500 because nothing is wrong with this
     * server, and the caller may well succeed later. The provider's own words
     * are logged in full but deliberately kept out of the response: a candidate
     * mid-interview should not be told about the platform's billing. An
     * administrator reads the real reason in the logs, or gets it verbatim from
     * Test connection on the AI engine screen.
     */
    @ExceptionHandler(value = ApiException.class)
    public ResponseEntity<ErrorResponse> handleAiProviderEx(ApiException e) {
        log.error(
                "AI provider rejected the request: code={} status={} message={}",
                e.code(), e.status(), e.message(), e
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .status(statusText(HttpStatus.SERVICE_UNAVAILABLE))
                .code(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message("The AI service is unavailable right now. Please try again shortly.")
                .timestamp(Instant.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledEx(Exception e) {
        // A provider failure that was retried arrives wrapped in a RetryException
        // rather than raw, so the chain is searched before this is called an
        // unexpected error. Without this a depleted quota would read as a bug in
        // this server.
        ApiException providerError = findProviderError(e);
        if (providerError != null) {
            return handleAiProviderEx(providerError);
        }

        log.error("Unhandled application exception", e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .status(statusText(HttpStatus.INTERNAL_SERVER_ERROR))
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Unexpected server error")
                .timestamp(Instant.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ApiException findProviderError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ApiException apiException) {
                return apiException;
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return null;
    }

    private String statusText(HttpStatusCode statusCode) {
        if (statusCode instanceof HttpStatus httpStatus) {
            return httpStatus.getReasonPhrase();
        }
        return statusCode.toString();
    }

}
