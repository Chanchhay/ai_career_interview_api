package co.istad.ai_interview_app.features.job.parsing.service;

import co.istad.ai_interview_app.features.file.FileVisibility;
import co.istad.ai_interview_app.features.file.service.FileStorageService;
import co.istad.ai_interview_app.features.job.dto.JobPostSectionRequest;
import co.istad.ai_interview_app.features.job.dto.ResolvedSkill;
import co.istad.ai_interview_app.features.job.dto.SkillCreateRequest;
import co.istad.ai_interview_app.features.job.entity.JobCategory;
import co.istad.ai_interview_app.features.job.parsing.dto.ExtractedJobDocument;
import co.istad.ai_interview_app.features.job.parsing.dto.ExtractedJobSection;
import co.istad.ai_interview_app.features.job.parsing.dto.ExtractedJobSkill;
import co.istad.ai_interview_app.features.job.parsing.dto.JobDocumentParseResponse;
import co.istad.ai_interview_app.features.job.parsing.dto.ParsedJobSkill;
import co.istad.ai_interview_app.features.job.repository.JobCategoryRepository;
import co.istad.ai_interview_app.features.job.service.RecruiterSkillService;
import co.istad.ai_interview_app.shared.enums.job.JobPostSectionType;
import co.istad.ai_interview_app.shared.exception.GeminiGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;
import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

/**
 * Recruiter-facing PDF job description import.
 *
 * <p>The order of work matters: the text is extracted <em>before</em> the file
 * is stored, so an unreadable upload is rejected without leaving an object in
 * MinIO that no job post will ever reference.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobDocumentParseServiceImpl implements JobDocumentParseService {

    /**
     * Values the frontend's selects offer. Anything the model returns outside
     * these sets is dropped rather than shown, since the form could not render
     * it as a selected option anyway.
     */
    private static final Set<String> JOB_TYPES = Set.of(
            "FULL_TIME", "PART_TIME", "CONTRACT", "INTERNSHIP", "TEMPORARY"
    );
    private static final Set<String> WORK_MODES = Set.of(
            "ONSITE", "HYBRID", "REMOTE"
    );
    private static final Set<String> EXPERIENCE_LEVELS = Set.of(
            "ENTRY", "JUNIOR", "MID", "SENIOR", "LEAD"
    );

    /**
     * Classifications a suggested skill may be created under. The model is
     * asked for one of these; anything else becomes null rather than seeding
     * the skills table with a one-off label.
     */
    private static final Set<String> SKILL_TYPES = Set.of(
            "LANGUAGE", "FRAMEWORK", "LIBRARY", "TOOL",
            "PLATFORM", "DATABASE", "METHODOLOGY", "DOMAIN"
    );

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_SECTION_TITLE_LENGTH = 150;
    private static final int MAX_LOCATION_LENGTH = 255;

    private final FileStorageService fileStorageService;
    private final PdfTextExtractor pdfTextExtractor;
    private final JobDocumentExtractor jobDocumentExtractor;
    private final JobCategoryRepository jobCategoryRepository;
    private final RecruiterSkillService recruiterSkillService;

    @Override
    public JobDocumentParseResponse parse(MultipartFile file) {
        byte[] pdf = readPdf(file);
        String documentText = pdfTextExtractor.extract(pdf);

        List<JobCategory> categories = jobCategoryRepository.findAllByOrderByNameAsc();

        ExtractedJobDocument extracted;
        try {
            extracted = jobDocumentExtractor.extract(
                    documentText,
                    categories.stream().map(JobCategory::getName).toList()
            );
        } catch (GeminiGenerationException e) {
            log.warn("Job description extraction failed", e);
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "This document could not be read as a job description. Try a different file, or fill the form in manually."
            );
        }

        JobCategory category = matchCategory(categories, extracted.categoryName());
        List<ParsedJobSkill> skills = resolveSkills(extracted.skills());
        BigDecimal[] salary = normalizeSalary(extracted.salaryMin(), extracted.salaryMax());

        // Stored last: everything above can reject the upload, and only a file
        // that produced a usable result is worth keeping.
        String sourceFileUrl = fileStorageService
                .upload(file, FileVisibility.PRIVATE)
                .url();

        return new JobDocumentParseResponse(
                sourceFileUrl,
                truncate(normalizeBlankToNull(extracted.title()), MAX_TITLE_LENGTH),
                normalizeBlankToNull(extracted.description()),
                truncate(normalizeBlankToNull(extracted.location()), MAX_LOCATION_LENGTH),
                normalizeChoice(extracted.jobType(), JOB_TYPES),
                normalizeChoice(extracted.workMode(), WORK_MODES),
                normalizeChoice(extracted.experienceLevel(), EXPERIENCE_LEVELS),
                salary[0],
                salary[1],
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                toSectionRequests(extracted.sections()),
                skills
        );
    }

    private byte[] readPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided");
        }

        // Narrower than the shared uploader, which also takes images: an image
        // has no text layer, so accepting one here only fails later and slower.
        if (!MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType())) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only PDF job descriptions can be parsed"
            );
        }

        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The uploaded file could not be read"
            );
        }
    }

    private String normalizeChoice(String value, Set<String> allowed) {
        String normalized = normalizeBlankToNull(value);
        if (normalized == null) {
            return null;
        }

        String candidate = normalized.toUpperCase(Locale.ROOT).replace(' ', '_');

        return allowed.contains(candidate) ? candidate : null;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    /**
     * @return a two-slot array of {min, max}, either of which may be null
     */
    private BigDecimal[] normalizeSalary(Double min, Double max) {
        BigDecimal normalizedMin = toPositiveAmount(min);
        BigDecimal normalizedMax = toPositiveAmount(max);

        // A range read out of a table now and then comes back the wrong way
        // round; the numbers are still right, so swap rather than discard.
        if (normalizedMin != null
                && normalizedMax != null
                && normalizedMin.compareTo(normalizedMax) > 0) {
            return new BigDecimal[]{normalizedMax, normalizedMin};
        }

        return new BigDecimal[]{normalizedMin, normalizedMax};
    }

    private BigDecimal toPositiveAmount(Double value) {
        if (value == null || value.isNaN() || value.isInfinite() || value <= 0) {
            return null;
        }

        return BigDecimal.valueOf(value).stripTrailingZeros();
    }

    private JobCategory matchCategory(List<JobCategory> categories, String categoryName) {
        String normalized = normalizeBlankToNull(categoryName);
        if (normalized == null) {
            return null;
        }

        return categories.stream()
                .filter(category -> category.getName() != null
                        && category.getName().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }

    /**
     * Turns the skills the document named into rows, creating the ones the
     * shared list is missing.
     *
     * <p>A job description is only as good as the skills on it, and a recruiter
     * should not have to re-enter what their own PDF already says. Everything
     * named is therefore attached, with the skills feature deduplicating by
     * name and recording which recruiter added anything new.
     */
    private List<ParsedJobSkill> resolveSkills(List<ExtractedJobSkill> extractedSkills) {
        if (extractedSkills == null || extractedSkills.isEmpty()) {
            return List.of();
        }

        List<SkillCreateRequest> requests = new ArrayList<>();
        for (ExtractedJobSkill skill : extractedSkills) {
            if (skill == null) {
                continue;
            }

            requests.add(new SkillCreateRequest(
                    skill.name(),
                    normalizeChoice(skill.skillType(), SKILL_TYPES)
            ));
        }

        return recruiterSkillService.findOrCreateAll(requests)
                .stream()
                .map(resolved -> new ParsedJobSkill(
                        resolved.skill().id(),
                        resolved.skill().name(),
                        resolved.skill().skillType(),
                        resolved.created()
                ))
                .toList();
    }

    /**
     * Keeps one section per type, in the enum's own order, so the form always
     * lays the blocks out the same way regardless of how the PDF was arranged.
     * {@code contentText} stays null here — the form recomputes the plain-text
     * mirror from whatever the recruiter ends up saving.
     */
    private List<JobPostSectionRequest> toSectionRequests(List<ExtractedJobSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return List.of();
        }

        Map<JobPostSectionType, ExtractedJobSection> byType =
                new LinkedHashMap<>();

        for (ExtractedJobSection section : sections) {
            if (section == null
                    || section.sectionType() == null
                    || !hasText(section.contentMarkdown())) {
                continue;
            }
            byType.putIfAbsent(section.sectionType(), section);
        }

        List<JobPostSectionRequest> requests = new ArrayList<>();
        int displayOrder = 0;

        for (JobPostSectionType type : JobPostSectionType.values()) {
            ExtractedJobSection section = byType.get(type);
            if (section == null) {
                continue;
            }

            String title = truncate(
                    normalizeBlankToNull(section.title()),
                    MAX_SECTION_TITLE_LENGTH
            );

            requests.add(new JobPostSectionRequest(
                    type,
                    title == null ? defaultTitle(type) : title,
                    normalizeBlankToNull(section.contentMarkdown()),
                    null,
                    displayOrder++
            ));
        }

        return requests;
    }

    private String defaultTitle(JobPostSectionType type) {
        return switch (type) {
            case DESCRIPTION -> "Description";
            case REQUIREMENT_RESPONSIBILITY -> "Requirements & responsibilities";
            case BENEFIT -> "Benefits";
            case QUALIFICATION -> "Qualifications";
            case NICE_TO_HAVE -> "Nice to have";
            case ABOUT_ROLE -> "About the role";
        };
    }

}
