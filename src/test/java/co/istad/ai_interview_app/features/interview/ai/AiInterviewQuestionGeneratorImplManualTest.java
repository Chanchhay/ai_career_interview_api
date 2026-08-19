package co.istad.ai_interview_app.features.interview.ai;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewGenerationConfig;
import co.istad.ai_interview_app.features.interview.ai.dto.GeneratedQuestionSet;
import co.istad.ai_interview_app.features.interview.ai.service.AiInterviewQuestionGenerator;
import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_GEMINI_TEST", matches = "true")
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class AiInterviewQuestionGeneratorImplManualTest {

    @Autowired
    private AiInterviewQuestionGenerator questionGenerator;

    /**
     * Deliberately not the default seven-question, ten-point shape: the point of
     * this test is that the generator follows whatever the admin configured, so
     * a passing run against the old hard-coded prompt would be a false pass.
     */
    @Test
    void generatesTheConfiguredNumberAndMixOfQuestions() {
        Map<InterviewQuestionType, Integer> distribution = new LinkedHashMap<>();
        distribution.put(InterviewQuestionType.TECHNICAL, 3);
        distribution.put(InterviewQuestionType.PROBLEM_SOLVING, 2);

        AiInterviewGenerationConfig config = new AiInterviewGenerationConfig(
                5,
                20,
                distribution,
                "Prefer questions about day-to-day work over textbook definitions."
        );

        GeneratedQuestionSet result = questionGenerator.generateQuestions(
                "Backend Java Developer",
                """
                        Build and maintain Spring Boot REST APIs for a job matching
                        and AI interview platform. The role requires secure API
                        design, relational database modeling, clean service layers,
                        and collaboration with frontend developers.
                        """,
                "Junior to Mid-level",
                List.of("Java", "Spring Boot", "PostgreSQL", "REST API", "Spring Security"),
                config
        );

        assertThat(result.questions()).hasSize(5);
        assertThat(result.questions())
                .allSatisfy(question -> {
                    assertThat(question.order()).isNotNull();
                    assertThat(question.type()).isNotNull();
                    assertThat(question.question()).isNotBlank();
                    assertThat(question.rubric()).isNotBlank();
                    assertThat(question.maxScore()).isEqualTo(20);
                });

        result.questions().forEach(question -> System.out.printf(
                "%d. [%s] %s%n",
                question.order(),
                question.type(),
                question.question()
        ));
    }
}
