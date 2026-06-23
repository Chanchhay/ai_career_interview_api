package co.istad.ai_interview_app.common.exception;

public record FiledErrorResponse(
        String filed,
        String reason
) {
}