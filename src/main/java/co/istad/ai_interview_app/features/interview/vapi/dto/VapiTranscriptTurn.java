package co.istad.ai_interview_app.features.interview.vapi.dto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * One spoken turn from a Vapi call, with the provider's role vocabulary already
 * normalised.
 *
 * <p>Vapi labels the assistant {@code bot} in {@code artifact.messages} and
 * {@code assistant} in the OpenAI-formatted copy of the same conversation. Both
 * are folded into {@link #ASSISTANT} at parse time so nothing downstream has to
 * know which artifact the turns were read from.
 */
public record VapiTranscriptTurn(
        String role,
        String text
) {

    public static final String ASSISTANT = "assistant";

    public static final String USER = "user";

    public boolean isAssistant() {
        return ASSISTANT.equals(role);
    }

    public boolean isUser() {
        return USER.equals(role);
    }

    /**
     * Folds a provider's role vocabulary into {@link #ASSISTANT} or {@link #USER}.
     *
     * @return null for roles that carry no interview content — system prompts and
     * tool traffic — so callers can drop them
     */
    public static String normalizeRole(String role) {
        if (role == null) {
            return null;
        }

        return switch (role.toLowerCase()) {
            case "bot", "assistant", "interviewer" -> ASSISTANT;
            case "user", "customer", "candidate" -> USER;
            default -> null;
        };
    }

    /**
     * Renders turns as the labelled script Gemini is asked to split.
     *
     * <p>Speaker labels are spelled out rather than left as role names: the model
     * is reasoning about an interview, and "Interviewer"/"Candidate" is the
     * vocabulary the prompt uses.
     */
    public static String toTranscript(List<VapiTranscriptTurn> turns) {
        if (turns == null) {
            return "";
        }

        return turns.stream()
                .map(turn -> (turn.isAssistant() ? "Interviewer: " : "Candidate: ") + turn.text())
                .collect(Collectors.joining("\n"));
    }
}
