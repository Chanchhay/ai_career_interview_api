package co.istad.ai_interview_app.features.job.parsing;

import co.istad.ai_interview_app.features.file.FileVisibility;
import co.istad.ai_interview_app.features.file.dto.FileUploadResponse;
import co.istad.ai_interview_app.features.file.service.FileStorageService;
import co.istad.ai_interview_app.features.job.entity.JobCategory;
import co.istad.ai_interview_app.features.job.parsing.dto.ExtractedJobDocument;
import co.istad.ai_interview_app.features.job.parsing.dto.ExtractedJobSection;
import co.istad.ai_interview_app.features.job.parsing.dto.ExtractedJobSkill;
import co.istad.ai_interview_app.features.job.parsing.dto.JobDocumentParseResponse;
import co.istad.ai_interview_app.features.job.parsing.dto.ParsedJobSkill;
import co.istad.ai_interview_app.features.job.parsing.service.JobDocumentExtractor;
import co.istad.ai_interview_app.features.job.parsing.service.JobDocumentParseServiceImpl;
import co.istad.ai_interview_app.features.job.parsing.service.PdfTextExtractor;
import co.istad.ai_interview_app.features.job.repository.JobCategoryRepository;
import co.istad.ai_interview_app.features.job.dto.ResolvedSkill;
import co.istad.ai_interview_app.features.job.dto.SkillCreateRequest;
import co.istad.ai_interview_app.features.job.dto.SkillResponse;
import co.istad.ai_interview_app.features.job.service.RecruiterSkillService;
import co.istad.ai_interview_app.shared.enums.job.JobPostSectionType;
import co.istad.ai_interview_app.shared.exception.GeminiGenerationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers what happens between the uploaded PDF and the prefill the recruiter's
 * form receives: which uploads are rejected, and how free-text model output is
 * pinned back to values the form and the database can actually take.
 */
class JobDocumentParseServiceTest {

    private FileStorageService fileStorageService;
    private JobDocumentExtractor jobDocumentExtractor;
    private JobCategoryRepository jobCategoryRepository;
    private RecruiterSkillService recruiterSkillService;
    private JobDocumentParseServiceImpl service;

    @BeforeEach
    void setUp() {
        fileStorageService = mock(FileStorageService.class);
        jobDocumentExtractor = mock(JobDocumentExtractor.class);
        jobCategoryRepository = mock(JobCategoryRepository.class);
        recruiterSkillService = mock(RecruiterSkillService.class);

        service = new JobDocumentParseServiceImpl(
                fileStorageService,
                new PdfTextExtractor(),
                jobDocumentExtractor,
                jobCategoryRepository,
                recruiterSkillService
        );

        when(jobCategoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
        when(recruiterSkillService.findOrCreateAll(anyList())).thenReturn(List.of());
        when(fileStorageService.upload(any(), any())).thenReturn(new FileUploadResponse(
                "private/2026/08/parsed.pdf",
                "/api/v1/files/private/2026/08/parsed.pdf",
                "job.pdf",
                1L,
                "application/pdf"
        ));
    }

    /** A PDF whose single page carries the given lines of selectable text. */
    private static MultipartFile pdf(String... lines) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                        11
                );
                content.setLeading(14);
                content.newLineAtOffset(40, 760);
                for (String line : lines) {
                    content.showText(line);
                    content.newLine();
                }
                content.endText();
            }

            document.save(out);
            return new MockMultipartFile(
                    "file", "job.pdf", "application/pdf", out.toByteArray()
            );
        }
    }

    /** Enough lines to clear the extractor's minimum text length. */
    private static MultipartFile readableJobPdf() throws IOException {
        String[] lines = new String[12];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = "We are hiring a senior backend engineer for our team in Phnom Penh.";
        }
        return pdf(lines);
    }

    private static ExtractedJobDocument extracted(
            String jobType,
            String workMode,
            String experienceLevel,
            Double salaryMin,
            Double salaryMax,
            String categoryName,
            List<ExtractedJobSkill> skills,
            List<ExtractedJobSection> sections
    ) {
        return new ExtractedJobDocument(
                "Senior Backend Engineer",
                "Own our payment services.",
                "Phnom Penh",
                jobType,
                workMode,
                experienceLevel,
                salaryMin,
                salaryMax,
                categoryName,
                skills,
                sections
        );
    }

    private static SkillResponse skillResponse(Long id, String name, String skillType) {
        return new SkillResponse(id, name, skillType, null, null, null, null);
    }

    private void stubExtraction(ExtractedJobDocument document) {
        when(jobDocumentExtractor.extract(anyString(), anyList())).thenReturn(document);
    }

    @Test
    void nonPdfUploadIsRejectedWithoutCallingTheModel() {
        MultipartFile docx = new MockMultipartFile(
                "file",
                "job.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> service.parse(docx))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.UNSUPPORTED_MEDIA_TYPE);

        verify(jobDocumentExtractor, never()).extract(anyString(), anyList());
        verify(fileStorageService, never()).upload(any(), any());
    }

    @Test
    void pdfWithoutSelectableTextIsRejectedAndNotStored() throws IOException {
        MultipartFile imageOnly = pdf("Scan");

        assertThatThrownBy(() -> service.parse(imageOnly))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.UNPROCESSABLE_CONTENT);

        verify(fileStorageService, never()).upload(any(), any());
    }

    @Test
    void failedExtractionLeavesNothingInStorage() throws IOException {
        when(jobDocumentExtractor.extract(anyString(), anyList()))
                .thenThrow(new GeminiGenerationException("no job data"));

        assertThatThrownBy(() -> service.parse(readableJobPdf()))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.UNPROCESSABLE_CONTENT);

        verify(fileStorageService, never()).upload(any(), any());
    }

    @Test
    void parsedDocumentIsStoredPrivatelyAndItsUrlReturned() throws IOException {
        stubExtraction(extracted(
                "FULL_TIME", "REMOTE", "SENIOR",
                null, null, null, List.of(), List.of()
        ));

        JobDocumentParseResponse response = service.parse(readableJobPdf());

        ArgumentCaptor<FileVisibility> visibility =
                ArgumentCaptor.forClass(FileVisibility.class);
        verify(fileStorageService).upload(any(), visibility.capture());

        assertThat(visibility.getValue()).isEqualTo(FileVisibility.PRIVATE);
        assertThat(response.sourceFileUrl())
                .isEqualTo("/api/v1/files/private/2026/08/parsed.pdf");
        assertThat(response.title()).isEqualTo("Senior Backend Engineer");
        assertThat(response.jobType()).isEqualTo("FULL_TIME");
        assertThat(response.workMode()).isEqualTo("REMOTE");
        assertThat(response.experienceLevel()).isEqualTo("SENIOR");
    }

    @Test
    void choicesOutsideTheFormsVocabularyAreDropped() throws IOException {
        stubExtraction(extracted(
                "FREELANCE", "work from home", "PRINCIPAL",
                null, null, null, List.of(), List.of()
        ));

        JobDocumentParseResponse response = service.parse(readableJobPdf());

        assertThat(response.jobType()).isNull();
        assertThat(response.workMode()).isNull();
        assertThat(response.experienceLevel()).isNull();
    }

    @Test
    void lowercaseChoicesAreNormalizedRatherThanDropped() throws IOException {
        stubExtraction(extracted(
                "full time", "remote", "mid",
                null, null, null, List.of(), List.of()
        ));

        JobDocumentParseResponse response = service.parse(readableJobPdf());

        assertThat(response.jobType()).isEqualTo("FULL_TIME");
        assertThat(response.workMode()).isEqualTo("REMOTE");
        assertThat(response.experienceLevel()).isEqualTo("MID");
    }

    @Test
    void salaryRangeReadBackwardsIsSwappedAndNonPositiveAmountsDropped() throws IOException {
        stubExtraction(extracted(
                null, null, null, 4000.0, 2500.0, null, List.of(), List.of()
        ));

        JobDocumentParseResponse response = service.parse(readableJobPdf());

        assertThat(response.salaryMin()).isEqualByComparingTo(BigDecimal.valueOf(2500));
        assertThat(response.salaryMax()).isEqualByComparingTo(BigDecimal.valueOf(4000));

        stubExtraction(extracted(
                null, null, null, 0.0, -1.0, null, List.of(), List.of()
        ));

        JobDocumentParseResponse zeroed = service.parse(readableJobPdf());

        assertThat(zeroed.salaryMin()).isNull();
        assertThat(zeroed.salaryMax()).isNull();
    }

    @Test
    void categoryIsMatchedByNameIgnoringCase() throws IOException {
        JobCategory engineering = new JobCategory();
        engineering.setId(7L);
        engineering.setName("Engineering");
        when(jobCategoryRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(engineering));

        stubExtraction(extracted(
                null, null, null, null, null,
                "engineering", List.of(), List.of()
        ));

        JobDocumentParseResponse response = service.parse(readableJobPdf());

        assertThat(response.categoryId()).isEqualTo(7L);
        assertThat(response.categoryName()).isEqualTo("Engineering");
    }

    @Test
    void unknownCategoryLeavesTheFieldEmpty() throws IOException {
        stubExtraction(extracted(
                null, null, null, null, null,
                "Underwater Basket Weaving", List.of(), List.of()
        ));

        JobDocumentParseResponse response = service.parse(readableJobPdf());

        assertThat(response.categoryId()).isNull();
        assertThat(response.categoryName()).isNull();
    }

    @Test
    void everySkillNamedByTheDocumentIsResolvedAndFlaggedIfItWasCreated() throws IOException {
        when(recruiterSkillService.findOrCreateAll(anyList())).thenReturn(List.of(
                new ResolvedSkill(skillResponse(3L, "React", "LIBRARY"), false),
                new ResolvedSkill(skillResponse(9L, "Zustand", "LIBRARY"), true)
        ));

        stubExtraction(extracted(
                null, null, null, null, null, null,
                List.of(
                        new ExtractedJobSkill("react", "LIBRARY"),
                        new ExtractedJobSkill("Zustand", "library"),
                        new ExtractedJobSkill("Vibes", "ENERGY")
                ),
                List.of()
        ));

        JobDocumentParseResponse response = service.parse(readableJobPdf());

        assertThat(response.skills()).containsExactly(
                new ParsedJobSkill(3L, "React", "LIBRARY", false),
                new ParsedJobSkill(9L, "Zustand", "LIBRARY", true)
        );

        // Types are normalized on the way in, and one the model invented is
        // dropped rather than becoming a label in the shared list.
        ArgumentCaptor<List<SkillCreateRequest>> requested =
                ArgumentCaptor.forClass(List.class);
        verify(recruiterSkillService).findOrCreateAll(requested.capture());
        assertThat(requested.getValue()).containsExactly(
                new SkillCreateRequest("react", "LIBRARY"),
                new SkillCreateRequest("Zustand", "LIBRARY"),
                new SkillCreateRequest("Vibes", null)
        );
    }

    @Test
    void sectionsAreDeduplicatedAndOrderedByType() throws IOException {
        stubExtraction(extracted(
                null, null, null, null, null, null, List.of(),
                List.of(
                        new ExtractedJobSection(
                                JobPostSectionType.BENEFIT, "Perks", "- Health cover"
                        ),
                        new ExtractedJobSection(
                                JobPostSectionType.REQUIREMENT_RESPONSIBILITY,
                                "What you'll do",
                                "- Ship services"
                        ),
                        // Dropped: same type as the section above.
                        new ExtractedJobSection(
                                JobPostSectionType.REQUIREMENT_RESPONSIBILITY,
                                "Duplicate",
                                "- Ignored"
                        ),
                        // Dropped: no body to show.
                        new ExtractedJobSection(
                                JobPostSectionType.QUALIFICATION, "Empty", "  "
                        )
                )
        ));

        JobDocumentParseResponse response = service.parse(readableJobPdf());

        assertThat(response.sections())
                .extracting("sectionType", "title", "displayOrder")
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(
                                JobPostSectionType.REQUIREMENT_RESPONSIBILITY,
                                "What you'll do",
                                0
                        ),
                        org.assertj.core.api.Assertions.tuple(
                                JobPostSectionType.BENEFIT, "Perks", 1
                        )
                );
    }
}
