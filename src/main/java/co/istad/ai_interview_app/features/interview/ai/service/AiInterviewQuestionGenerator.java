package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.features.interview.ai.dto.GeneratedQuestionSet;

import java.util.List;

public interface AiInterviewQuestionGenerator {

    GeneratedQuestionSet generateQuestions(
            String jobTitle,
            String jobDescription,
            String experienceLevel,
            List<String> requiredSkills
    );
}