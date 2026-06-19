package co.istad.ai_interview_app.shared.exception;

public record FiledErrorResponse(
        String filed,
        String reason
) {
}