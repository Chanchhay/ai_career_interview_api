package co.istad.ai_interview_app.shared.exception;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;

@Builder
public record ErrorResponse(
        String status,
        Integer code,
        String message,
        Instant timestamp,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Object errorDetail
) {
}