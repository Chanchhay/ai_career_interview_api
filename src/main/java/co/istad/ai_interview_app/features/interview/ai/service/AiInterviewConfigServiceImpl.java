package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.features.admin.entity.SystemSetting;
import co.istad.ai_interview_app.features.admin.repository.SystemSettingRepository;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewConfigRequest;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewConfigResponse;
import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewGenerationConfig;
import co.istad.ai_interview_app.features.interview.ai.dto.QuestionTypeAllocation;
import co.istad.ai_interview_app.shared.enums.admin.SettingValueType;
import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

/**
 * Stores the AI interview settings as rows in {@code system_settings}, one key
 * per setting, so they can be changed without a deployment.
 *
 * <p>Nothing is seeded: an untouched install has no rows and falls back to the
 * defaults below, which are the values that used to be compiled into the
 * generator. A key that has been corrupted by hand falls back the same way and
 * logs, because a bad row must not stop candidates interviewing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiInterviewConfigServiceImpl implements AiInterviewConfigService {

    private static final String CATEGORY = "AI_INTERVIEW";

    private static final String KEY_QUESTION_COUNT = "ai_interview.question_count";
    private static final String KEY_MAX_SCORE = "ai_interview.max_score_per_question";
    private static final String KEY_TYPE_DISTRIBUTION = "ai_interview.type_distribution";
    private static final String KEY_EXTRA_INSTRUCTIONS = "ai_interview.additional_instructions";

    /** What the generator asked for before these settings existed. */
    private static final int DEFAULT_QUESTION_COUNT = 7;
    private static final int DEFAULT_MAX_SCORE = 10;
    private static final Map<InterviewQuestionType, Integer> DEFAULT_DISTRIBUTION = defaultDistribution();

    private static Map<InterviewQuestionType, Integer> defaultDistribution() {
        Map<InterviewQuestionType, Integer> defaults = new LinkedHashMap<>();
        defaults.put(InterviewQuestionType.TECHNICAL, 4);
        defaults.put(InterviewQuestionType.BEHAVIORAL, 2);
        defaults.put(InterviewQuestionType.SITUATIONAL, 1);
        return Collections.unmodifiableMap(defaults);
    }

    /**
     * Owned rather than injected: the application context publishes no
     * {@code ObjectMapper} bean, and this reads one small map of enum to int —
     * a use that wants plain Jackson defaults, not the HTTP layer's settings.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SystemSettingRepository settingRepository;

    @Override
    @Transactional(readOnly = true)
    public AiInterviewConfigResponse getConfig() {
        AiInterviewGenerationConfig config = currentGenerationConfig();

        Instant updatedAt = null;
        String updatedBy = null;
        for (SystemSetting setting : settingRepository.findAllByCategory(CATEGORY)) {
            if (setting.getUpdatedAt() != null
                    && (updatedAt == null || setting.getUpdatedAt().isAfter(updatedAt))) {
                updatedAt = setting.getUpdatedAt();
                updatedBy = setting.getUpdatedBy();
            }
        }

        return new AiInterviewConfigResponse(
                config.questionCount(),
                config.maxScorePerQuestion(),
                toAllocations(config.typeDistribution()),
                config.additionalInstructions(),
                Arrays.asList(InterviewQuestionType.values()),
                updatedAt,
                updatedBy
        );
    }

    @Override
    @Transactional
    public AiInterviewConfigResponse updateConfig(AiInterviewConfigRequest request) {
        Map<InterviewQuestionType, Integer> distribution = validateDistribution(
                request.typeDistribution(),
                request.questionCount()
        );

        writeSetting(
                KEY_QUESTION_COUNT,
                String.valueOf(request.questionCount()),
                SettingValueType.NUMBER,
                "How many questions each generated AI interview contains."
        );
        writeSetting(
                KEY_MAX_SCORE,
                String.valueOf(request.maxScorePerQuestion()),
                SettingValueType.NUMBER,
                "The score a perfect answer to a single question is worth."
        );
        writeSetting(
                KEY_TYPE_DISTRIBUTION,
                writeJson(distribution),
                SettingValueType.JSON,
                "How many questions of each interview question type to generate."
        );
        writeSetting(
                KEY_EXTRA_INSTRUCTIONS,
                normalizeBlankToNull(request.additionalInstructions()),
                SettingValueType.STRING,
                "Extra wording appended to the question-generation prompt."
        );

        return getConfig();
    }

    @Override
    @Transactional(readOnly = true)
    public AiInterviewGenerationConfig currentGenerationConfig() {
        int questionCount = readInt(KEY_QUESTION_COUNT, DEFAULT_QUESTION_COUNT);
        int maxScore = readInt(KEY_MAX_SCORE, DEFAULT_MAX_SCORE);
        Map<InterviewQuestionType, Integer> distribution = readDistribution();

        int allocated = distribution.values().stream().mapToInt(Integer::intValue).sum();
        if (allocated != questionCount) {
            // Only reachable if a row was edited outside the API. The stored
            // count is what the rest of the system validates against, so the
            // distribution is the side that gives way.
            log.warn(
                    "AI interview type distribution allocates {} of {} questions; generating by count alone",
                    allocated, questionCount
            );
            distribution = Map.of();
        }

        return new AiInterviewGenerationConfig(
                questionCount,
                maxScore,
                distribution,
                readSetting(KEY_EXTRA_INSTRUCTIONS).orElse(null)
        );
    }

    /* ------------------------------------------------------------ helpers --- */

    private Map<InterviewQuestionType, Integer> validateDistribution(
            List<QuestionTypeAllocation> allocations,
            int questionCount
    ) {
        Map<InterviewQuestionType, Integer> distribution = new LinkedHashMap<>();

        for (QuestionTypeAllocation allocation : allocations) {
            if (allocation.count() == 0) {
                continue;
            }
            if (distribution.putIfAbsent(allocation.type(), allocation.count()) != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Question type " + allocation.type() + " is listed twice"
                );
            }
        }

        if (distribution.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one question type must be given a question"
            );
        }

        int allocated = distribution.values().stream().mapToInt(Integer::intValue).sum();
        if (allocated != questionCount) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question types allocate " + allocated + " questions but the interview length is " + questionCount
            );
        }

        return distribution;
    }

    private void writeSetting(
            String key,
            String value,
            SettingValueType valueType,
            String description
    ) {
        SystemSetting setting = settingRepository.findBySettingKey(key)
                .orElseGet(() -> {
                    SystemSetting created = new SystemSetting();
                    created.setSettingKey(key);
                    return created;
                });

        if (Boolean.FALSE.equals(setting.getEditable())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Setting '" + key + "' is locked and cannot be changed"
            );
        }

        setting.setSettingValue(value);
        setting.setValueType(valueType);
        setting.setCategory(CATEGORY);
        setting.setDescription(description);

        settingRepository.save(setting);
    }

    private Optional<String> readSetting(String key) {
        return settingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .map(value -> normalizeBlankToNull(value));
    }

    private int readInt(String key, int fallback) {
        return readSetting(key)
                .map(value -> {
                    try {
                        return Integer.parseInt(value.trim());
                    } catch (NumberFormatException ex) {
                        log.warn("Setting {} holds '{}', which is not a number; using {}", key, value, fallback);
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    private Map<InterviewQuestionType, Integer> readDistribution() {
        Optional<String> json = readSetting(KEY_TYPE_DISTRIBUTION);
        if (json.isEmpty()) {
            return DEFAULT_DISTRIBUTION;
        }

        try {
            return OBJECT_MAPPER.readValue(
                    json.get(),
                    new TypeReference<LinkedHashMap<InterviewQuestionType, Integer>>() {
                    }
            );
        } catch (Exception ex) {
            log.warn("Setting {} holds unreadable JSON; using the default distribution", KEY_TYPE_DISTRIBUTION, ex);
            return DEFAULT_DISTRIBUTION;
        }
    }

    private String writeJson(Map<InterviewQuestionType, Integer> distribution) {
        try {
            return OBJECT_MAPPER.writeValueAsString(distribution);
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not store the question type distribution"
            );
        }
    }

    private List<QuestionTypeAllocation> toAllocations(Map<InterviewQuestionType, Integer> distribution) {
        List<QuestionTypeAllocation> allocations = new ArrayList<>();
        distribution.forEach((type, count) -> allocations.add(new QuestionTypeAllocation(type, count)));
        return allocations;
    }
}
