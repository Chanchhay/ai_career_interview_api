package co.istad.ai_interview_app.features.interview.ai.service;

import co.istad.ai_interview_app.features.interview.ai.dto.AiInterviewGenerationConfig;
import co.istad.ai_interview_app.features.interview.ai.dto.GeneratedQuestion;
import co.istad.ai_interview_app.features.interview.ai.dto.GeneratedQuestionSet;
import co.istad.ai_interview_app.features.interview.question.repository.JobInterviewQuestionRepository;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.job.entity.JobPostSection;
import co.istad.ai_interview_app.shared.enums.interview.InterviewQuestionType;
import co.istad.ai_interview_app.shared.enums.interview.ManualQuestionMode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

/**
 * Decides which questions an interview asks: the ones an administrator wrote
 * for the job, plus whatever the AI is still asked to add.
 *
 * <p>Pulled out of the interview service so the candidate flow and the guest
 * flow cannot drift apart. Both compose questions the same way, and an
 * administrator who writes a question for a job sees it asked in either.
 *
 * <p>Holds no authorisation: who may interview is decided by the callers.
 */
@Component
@RequiredArgsConstructor
public class InterviewQuestionComposer {

    private final AiInterviewQuestionGenerator questionGenerator;
    private final JobInterviewQuestionRepository writtenQuestionRepository;

    /** The parts of a job the generator is told about. */
    public record JobFacts(
            String title,
            String description,
            String experienceLevel,
            List<String> requiredSkills
    ) {
    }

    /**
     * Everything the generator and the evaluator are told about a job.
     *
     * <p>Lives here so a guest interview and a candidate's interview describe
     * the same job in the same words — the questions would otherwise be subtly
     * different for no reason anyone chose.
     */
    public JobFacts jobFacts(JobPost jobPost) {
        String sectionText = jobPost.getSections()
                .stream()
                .sorted(Comparator.comparing(JobPostSection::getDisplayOrder))
                .flatMap(section -> Stream.of(section.getTitle(), section.getContentText(), section.getContentMarkdown()))
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("\n\n"));

        String description = Stream.of(jobPost.getDescription(), sectionText)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("\n\n"));

        List<String> skills = jobPost.getSkills()
                .stream()
                .map(jobPostSkill -> jobPostSkill.getSkill().getName())
                .filter(skill -> skill != null && !skill.isBlank())
                .sorted()
                .toList();

        return new JobFacts(jobPost.getTitle(), description, jobPost.getExperienceLevel(), skills);
    }

    /**
     * The questions this session will ask: what an administrator wrote for the
     * job, then whatever the AI is still asked to add.
     *
     * <p>Written questions always come first and keep their authored order — an
     * author who put a screening question at the top meant it to be asked first.
     */
    public List<GeneratedQuestion> compose(
            JobFacts job,
            ManualQuestionMode manualQuestionMode,
            List<GeneratedQuestion> written,
            AiInterviewGenerationConfig config
    ) {

        int generatedCount = written.isEmpty()
                ? config.questionCount()
                : manualQuestionMode == ManualQuestionMode.MANUAL_ONLY
                        ? 0
                        : Math.max(0, config.questionCount() - written.size());

        // Nothing left for the AI: MANUAL_ONLY, or the written set already fills
        // the interview. Either way it is not called at all.
        if (generatedCount == 0) return renumber(written);

        AiInterviewGenerationConfig generationConfig = written.isEmpty()
                ? config
                : topUpConfig(config, written, generatedCount);

        GeneratedQuestionSet generated = questionGenerator.generateQuestions(
                job.title(),
                job.description(),
                job.experienceLevel(),
                job.requiredSkills(),
                generationConfig
        );

        validateGeneratedQuestions(generated, generationConfig);

        List<GeneratedQuestion> composed = new ArrayList<>(written);
        composed.addAll(generated.questions());

        return renumber(composed);
    }

    /**
     * The generation settings for the part the AI still has to write.
     *
     * <p>Each written question is taken off its own type's allocation first, so
     * a job with two hand-written behavioural questions gets two fewer generated
     * ones of that type rather than a lopsided interview. What that leaves is
     * then nudged up or down to land exactly on the number still needed, because
     * the validator refuses a set that does not match its own distribution.
     */
    private AiInterviewGenerationConfig topUpConfig(
            AiInterviewGenerationConfig config,
            List<GeneratedQuestion> written,
            int generatedCount
    ) {
        Map<InterviewQuestionType, Integer> distribution = new LinkedHashMap<>(config.typeDistribution());

        for (GeneratedQuestion question : written) {
            distribution.computeIfPresent(question.type(), (type, count) -> Math.max(0, count - 1));
        }

        /*
         * Every allocation can be used up while questions are still owed — a job
         * with more written questions of one type than the mix allows. Falling
         * back to the mix's first type keeps a valid request rather than asking
         * for zero questions of everything.
         */
        if (distribution.values().stream().mapToInt(Integer::intValue).sum() == 0) {
            InterviewQuestionType fallback = config.typeDistribution().keySet().stream()
                    .findFirst()
                    .orElse(InterviewQuestionType.GENERAL);
            distribution = new LinkedHashMap<>(Map.of(fallback, generatedCount));
        }

        balance(distribution, generatedCount);

        return new AiInterviewGenerationConfig(
                generatedCount,
                config.maxScorePerQuestion(),
                distribution,
                appendWrittenQuestions(config.additionalInstructions(), written)
        );
    }

    /** Adds or removes one at a time until the allocations total {@code target}. */
    private void balance(Map<InterviewQuestionType, Integer> distribution, int target) {
        int allocated = distribution.values().stream().mapToInt(Integer::intValue).sum();

        while (allocated != target) {
            InterviewQuestionType type = allocated < target
                    ? largest(distribution)
                    : largestAbove(distribution);

            distribution.merge(type, allocated < target ? 1 : -1, Integer::sum);
            allocated += allocated < target ? 1 : -1;
        }
    }

    private InterviewQuestionType largest(Map<InterviewQuestionType, Integer> distribution) {
        return distribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(InterviewQuestionType.GENERAL);
    }

    private InterviewQuestionType largestAbove(Map<InterviewQuestionType, Integer> distribution) {
        return distribution.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(InterviewQuestionType.GENERAL);
    }

    /**
     * Tells the model what a human already asked, so it writes around it.
     *
     * <p>Without this the generated half happily repeats a written question in
     * different words, and the candidate answers the same thing twice.
     */
    private String appendWrittenQuestions(String instructions, List<GeneratedQuestion> written) {
        StringBuilder text = new StringBuilder();

        if (normalizeBlankToNull(instructions) != null) {
            text.append(instructions.trim()).append("\n\n");
        }

        text.append("These questions are already being asked by a human interviewer. ")
                .append("Do not repeat them or ask a reworded version of them:\n");

        for (GeneratedQuestion question : written) {
            text.append("- ").append(question.question()).append('\n');
        }

        return text.toString();
    }

    /** Renumbers a composed list so the session's order is 1..n and contiguous. */
    private List<GeneratedQuestion> renumber(List<GeneratedQuestion> questions) {
        List<GeneratedQuestion> ordered = new ArrayList<>(questions.size());
        int order = 1;

        for (GeneratedQuestion question : questions) {
            ordered.add(new GeneratedQuestion(
                    order++,
                    question.type(),
                    question.question(),
                    question.rubric(),
                    question.maxScore()
            ));
        }

        return ordered;
    }

    /** A job's hand-written questions, in the shape the session builder uses. */
    public List<GeneratedQuestion> writtenQuestions(Long jobPostId) {
        return writtenQuestionRepository.findAllByJobPost_IdOrderByDisplayOrderAsc(jobPostId)
                .stream()
                .map(question -> new GeneratedQuestion(
                        question.getDisplayOrder(),
                        question.getQuestionType(),
                        question.getQuestionText(),
                        question.getExpectedAnswer(),
                        question.getMaxScore()
                ))
                .toList();
    }

    /**
     * Refuses an AI answer that does not match what was asked for.
     *
     * <p>Only the generated part is checked: hand-written questions are the
     * author's, and holding them to the model's contract would reject a
     * perfectly good question for being worth the wrong number of points.
     */
    void validateGeneratedQuestions(
            GeneratedQuestionSet generatedQuestionSet,
            AiInterviewGenerationConfig config
    ) {
        if (generatedQuestionSet == null || generatedQuestionSet.questions() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI did not return questions");
        }

        if (generatedQuestionSet.questions().size() != config.questionCount()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI did not return exactly " + config.questionCount() + " questions"
            );
        }

        Set<Integer> orders = new HashSet<>();
        for (GeneratedQuestion question : generatedQuestionSet.questions()) {
            if (question == null
                    || question.order() == null
                    || !orders.add(question.order())
                    || question.type() == null
                    || normalizeBlankToNull(question.question()) == null
                    || normalizeBlankToNull(question.rubric()) == null
                    || question.maxScore() == null
                    || question.maxScore() != config.maxScorePerQuestion()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned invalid questions");
            }
        }
    }
}
