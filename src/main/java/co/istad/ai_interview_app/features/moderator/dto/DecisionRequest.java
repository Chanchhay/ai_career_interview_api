package co.istad.ai_interview_app.features.moderator.dto;

import jakarta.validation.constraints.Size;

public record DecisionRequest(
        @Size(max = 5000, message = "Decision note must be at most 5000 characters")
        String decisionNote
) {
}
