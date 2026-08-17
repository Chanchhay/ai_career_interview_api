package co.istad.ai_interview_app.features.interview.ai.dto;

import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The generation settings as the generator and the interview service consume
 * them: no audit fields, no enum catalogue, and the type distribution already
 * reduced to the types that actually carry questions.
 */
public record AiInterviewGenerationConfig(
        int questionCount,
        int maxScorePerQuestion,
        Map<InterviewQuestionType, Integer> typeDistribution,
        String additionalInstructions
) {

    public AiInterviewGenerationConfig {
        Map<InterviewQuestionType, Integer> used = new LinkedHashMap<>();
        typeDistribution.forEach((type, count) -> {
            if (count != null && count > 0) {
                used.put(type, count);
            }
        });
        // Insertion order is kept on purpose: it decides the order the types are
        // listed in the prompt, and a stable prompt keeps generations comparable.
        typeDistribution = Collections.unmodifiableMap(used);
    }

    public Set<InterviewQuestionType> allowedTypes() {
        return typeDistribution.keySet();
    }
}
