package co.istad.ai_interview_app.features.common.response;

public record ApiResponse<T>(
        Boolean success,
        String message,
        T data
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Request completed successfully", data);
    }
}
