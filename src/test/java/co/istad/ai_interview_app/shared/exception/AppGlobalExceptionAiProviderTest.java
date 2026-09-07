package co.istad.ai_interview_app.shared.exception;

import com.google.genai.errors.ClientException;
import org.junit.jupiter.api.Test;
import org.springframework.core.retry.RetryException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A provider refusal is not this server's fault and must not read like one —
 * and it must not tell a candidate about the platform's billing either.
 */
class AppGlobalExceptionAiProviderTest {

    private final AppGlobalException handler = new AppGlobalException();

    private static final String BILLING_MESSAGE =
            "Your prepayment credits are depleted. Please go to AI Studio";

    @Test
    void answersProviderRefusalsWithServiceUnavailableAndNoProviderDetail() {
        ResponseEntity<ErrorResponse> response = handler.handleAiProviderEx(
                new ClientException(429, "RESOURCE_EXHAUSTED", BILLING_MESSAGE)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("The AI service is unavailable right now. Please try again shortly.");
        assertThat(response.getBody().message()).doesNotContain("credits");
        assertThat(response.getBody().message()).doesNotContain("billing");
    }

    @Test
    void findsTheProviderErrorEvenWhenTheRetryLayerHasWrappedIt() {
        Exception wrapped = new IllegalStateException(
                "chat call failed",
                new RetryException(
                        "Retry policy exhausted",
                        new ClientException(429, "RESOURCE_EXHAUSTED", BILLING_MESSAGE)
                )
        );

        ResponseEntity<ErrorResponse> response = handler.handleUnhandledEx(wrapped);

        // Not a 500: the cause chain is searched before anything is called a bug.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void stillReportsAGenuineServerBugAsFiveHundred() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnhandledEx(new IllegalStateException("a real bug"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void answersUnsupportedMethodWithMethodNotAllowedAndAllowHeader() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("GET", List.of("POST", "PATCH"));

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupportedEx(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getHeaders().getAllow())
                .containsExactlyInAnyOrder(HttpMethod.POST, HttpMethod.PATCH);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(405);
        assertThat(response.getBody().status()).isEqualTo("Method Not Allowed");
        assertThat(response.getBody().message()).isEqualTo("Request method 'GET' is not supported");
    }

    @Test
    void answersResourceNotFoundWithNotFound() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "Resource not found", "/api/v1/missing");

        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFoundEx(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(404);
        assertThat(response.getBody().message()).contains("/api/v1/missing");
    }

    @Test
    void answersUnsupportedMediaType() {
        HttpMediaTypeNotSupportedException ex =
                new HttpMediaTypeNotSupportedException("text/plain is not supported");

        ResponseEntity<ErrorResponse> response = handler.handleMediaTypeNotSupportedEx(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(415);
    }

    @Test
    void answersNotAcceptableMediaType() {
        HttpMediaTypeNotAcceptableException ex =
                new HttpMediaTypeNotAcceptableException("not acceptable");

        ResponseEntity<ErrorResponse> response = handler.handleMediaTypeNotAcceptableEx(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(406);
    }
}
