package co.istad.ai_interview_app.features.common.exception;

public record FiledErrorResponse(
        String filed,
        String reason
) {
}