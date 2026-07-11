package co.istad.ai_interview_app.shared.exception;

public record FieldErrorResponse(
        String field,
        String reason
) {
}
