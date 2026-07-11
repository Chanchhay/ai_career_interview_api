package co.istad.ai_interview_app.shared.util;

public final class TextUtils {

    private TextUtils() {
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static String normalizeBlankToNull(String value) {
        if (!hasText(value)) {
            return null;
        }

        return value.trim();
    }
}
