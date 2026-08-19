package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.features.admin.repository.SystemSettingRepository;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewConfigRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewConfigResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewGenerationConfig;
import co.istad.ai_interview_app.features.interview.ai.dto.QuestionTypeAllocation;
import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AiInterviewConfigServiceImplTest {

    @Autowired
    private AiInterviewConfigService configService;

    @Autowired
    private SystemSettingRepository settingRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * These settings are rows, not context state, so they outlive the test that
     * wrote them and would change what every other test in the shared context
     * generates.
     */
    @AfterEach
    void clearStoredSettings() {
        transactionTemplate.executeWithoutResult(status ->
                settingRepository.deleteAll(settingRepository.findAllByCategory("AI_INTERVIEW")));
    }

    @Test
    void fallsBackToTheDefaultShapeWhenNothingHasBeenSaved() {
        AiInterviewConfigResponse config = configService.getConfig();

        assertThat(config.questionCount()).isEqualTo(7);
        assertThat(config.maxScorePerQuestion()).isEqualTo(10);
        assertThat(config.typeDistribution()).containsExactly(
                new QuestionTypeAllocation(InterviewQuestionType.TECHNICAL, 4),
                new QuestionTypeAllocation(InterviewQuestionType.BEHAVIORAL, 2),
                new QuestionTypeAllocation(InterviewQuestionType.SITUATIONAL, 1)
        );
        assertThat(config.additionalInstructions()).isNull();
        assertThat(config.availableTypes()).containsExactly(InterviewQuestionType.values());
        assertThat(config.updatedAt()).isNull();
    }

    @Test
    void savedSettingsAreWhatTheNextGenerationIsHanded() {
        configService.updateConfig(new AiInterviewConfigRequest(
                4,
                20,
                List.of(
                        new QuestionTypeAllocation(InterviewQuestionType.PROBLEM_SOLVING, 3),
                        new QuestionTypeAllocation(InterviewQuestionType.COMMUNICATION, 1),
                        // Zero means "not asked", not "invalid": the console sends
                        // a row for every type it knows about.
                        new QuestionTypeAllocation(InterviewQuestionType.TECHNICAL, 0)
                ),
                "  Keep the questions practical.  "
        ));

        AiInterviewGenerationConfig generation = configService.currentGenerationConfig();

        assertThat(generation.questionCount()).isEqualTo(4);
        assertThat(generation.maxScorePerQuestion()).isEqualTo(20);
        assertThat(generation.typeDistribution()).containsExactlyInAnyOrderEntriesOf(Map.of(
                InterviewQuestionType.PROBLEM_SOLVING, 3,
                InterviewQuestionType.COMMUNICATION, 1
        ));
        assertThat(generation.additionalInstructions()).isEqualTo("Keep the questions practical.");

        AiInterviewConfigResponse readBack = configService.getConfig();
        assertThat(readBack.questionCount()).isEqualTo(4);
        assertThat(readBack.typeDistribution())
                .extracting(QuestionTypeAllocation::type)
                .doesNotContain(InterviewQuestionType.TECHNICAL);
        assertThat(readBack.updatedAt()).isNotNull();
    }

    @Test
    void rejectsATypeMixThatDoesNotAddUpToTheQuestionCount() {
        AiInterviewConfigRequest request = new AiInterviewConfigRequest(
                7,
                10,
                List.of(new QuestionTypeAllocation(InterviewQuestionType.TECHNICAL, 3)),
                null
        );

        assertThatThrownBy(() -> configService.updateConfig(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("allocate");

        assertThat(configService.getConfig().questionCount()).isEqualTo(7);
    }
}
