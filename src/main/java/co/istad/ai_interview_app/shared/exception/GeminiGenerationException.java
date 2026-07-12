package co.istad.ai_interview_app.shared.exception;

public class GeminiGenerationException extends RuntimeException {

    public GeminiGenerationException(String message) {
        super(message);
    }

    public GeminiGenerationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}