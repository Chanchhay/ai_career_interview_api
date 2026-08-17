package co.istad.ai_interview_app.shared.exception;

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

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledEx(Exception e) {
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

    private String statusText(HttpStatusCode statusCode) {
        if (statusCode instanceof HttpStatus httpStatus) {
            return httpStatus.getReasonPhrase();
        }
        return statusCode.toString();
    }

}
