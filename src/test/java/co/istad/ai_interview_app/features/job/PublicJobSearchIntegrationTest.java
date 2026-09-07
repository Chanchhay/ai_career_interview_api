package co.istad.ai_interview_app.features.job;

import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.identity.entity.UserAccount;
import co.istad.ai_interview_app.features.job.entity.JobCategory;
import co.istad.ai_interview_app.features.job.entity.JobPost;
import co.istad.ai_interview_app.features.job.entity.JobPostSkill;
import co.istad.ai_interview_app.features.job.entity.Skill;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.shared.enums.job.JobStatus;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import co.istad.ai_interview_app.shared.enums.visibility.VerificationStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;

/**
 * Covers the public job listing's filters and paging. Every request carries the
 * run's own marker as the keyword so the assertions see only the jobs this test
 * created, whatever else the shared test database holds.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicJobSearchIntegrationTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String marker;
    private UUID backendParentId;
    private UUID javaParentId;
    private UUID backendCategoryId;
    private UUID designCategoryId;
    private UUID javaSkillId;
    private UUID reactSkillId;
    private UUID seniorBackendJobId;
    private UUID juniorBackendJobId;
    private UUID remoteDesignJobId;
    private UUID internJobId;

    @BeforeEach
    void seed() {
        marker = "search-" + SEQUENCE.incrementAndGet();

        transactionTemplate.executeWithoutResult(status -> {
            UserAccount user = new UserAccount();
            user.setKeycloakUserId("recruiter-" + marker);
            entityManager.persist(user);

            RecruiterProfile recruiterProfile = new RecruiterProfile();
            recruiterProfile.setUserAccount(user);
            entityManager.persist(recruiterProfile);

            Company company = new Company();
            company.setRecruiterProfile(recruiterProfile);
            company.setName("Company " + marker);
            company.setVerificationStatus(VerificationStatus.APPROVED);
            company.setStatus(ProfileStatus.ACTIVE);
            entityManager.persist(company);

            JobCategory backend = persistCategory("Backend " + marker);
            JobCategory design = persistCategory("Design " + marker);
            backendParentId = backend.getParent().getId();
            backendCategoryId = backend.getId();
            designCategoryId = design.getId();

            Skill java = persistSkill("Java " + marker);
            Skill react = persistSkill("React " + marker);
            javaParentId = java.getParent().getId();
            javaSkillId = java.getId();
            reactSkillId = react.getId();

            JobPost seniorBackend = persistJob(recruiterProfile, company, backend, "Senior Backend " + marker,
                    "FULL_TIME", "ONSITE", "SENIOR", new BigDecimal("2000"), new BigDecimal("3000"), 4);
            // Two skills on one job: the join must not turn it into two rows.
            attachSkill(seniorBackend, java);
            attachSkill(seniorBackend, react);

            JobPost juniorBackend = persistJob(recruiterProfile, company, backend, "Junior Backend " + marker,
                    "PART_TIME", "HYBRID", "JUNIOR", new BigDecimal("500"), new BigDecimal("900"), 3);
            attachSkill(juniorBackend, java);

            JobPost remoteDesign = persistJob(recruiterProfile, company, design, "Remote Designer " + marker,
                    // Stored in a different case than the client sends it.
                    "full_time", "remote", "MID", new BigDecimal("1200"), null, 2);

            JobPost intern = persistJob(recruiterProfile, company, design, "Design Intern " + marker,
                    "INTERNSHIP", "ONSITE", "ENTRY", null, null, 1);

            entityManager.flush();

            seniorBackendJobId = seniorBackend.getId();
            juniorBackendJobId = juniorBackend.getId();
            remoteDesignJobId = remoteDesign.getId();
            internJobId = intern.getId();
        });
    }

    @Test
    void listsEveryPublishedJobNewestFirstByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/public/jobs").param("keyword", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(4))
                .andExpect(jsonPath("$.data.content[0].id").value(internJobId.toString()))
                .andExpect(jsonPath("$.data.content[3].id").value(seniorBackendJobId.toString()));
    }

    @Test
    void jobTypeAndWorkModeFiltersMatchAnyValueIgnoringCase() throws Exception {
        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("keyword", marker)
                        .param("jobType", "FULL_TIME", "INTERNSHIP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(3))
                .andExpect(jsonPath("$.data.content[*].id")
                        .value(org.hamcrest.Matchers.containsInAnyOrder(
                                seniorBackendJobId.toString(),
                                remoteDesignJobId.toString(),
                                internJobId.toString()
                        )));

        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("keyword", marker)
                        .param("workMode", "REMOTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(remoteDesignJobId.toString()));
    }

    /** The wire format the browser sends: one parameter, values comma joined. */
    @Test
    void commaJoinedValuesBindAsAListJustLikeRepeatedParameters() throws Exception {
        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("keyword", marker)
                        .param("jobType", "FULL_TIME,INTERNSHIP")
                        .param("categoryId", backendCategoryId + "," + designCategoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(3));
    }

    @Test
    void categoryExperienceAndSalaryFiltersNarrowTheListing() throws Exception {
        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("keyword", marker)
                        .param("categoryId", String.valueOf(backendCategoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(2));

        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("keyword", marker)
                        .param("categoryId", String.valueOf(backendCategoryId), String.valueOf(designCategoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(4));

        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("keyword", marker)
                        .param("experienceLevel", "SENIOR", "JUNIOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(2));

        // The 1200-only job is kept on its lone bound; the job with no salary
        // at all has nothing to compare and drops out.
        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("keyword", marker)
                        .param("salaryMin", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[*].id")
                        .value(org.hamcrest.Matchers.containsInAnyOrder(
                                seniorBackendJobId.toString(),
                                remoteDesignJobId.toString()
                        )));

        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("keyword", marker)
                        .param("salaryMax", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(juniorBackendJobId.toString()));
    }

    @Test
    void skillFilterMatchesAnySkillWithoutDuplicatingAJob() throws Exception {
        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("keyword", marker)
                        .param("skillIds", String.valueOf(javaSkillId), String.valueOf(reactSkillId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    void filtersComposeWithAnd() throws Exception {
        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("keyword", marker)
                        .param("categoryId", String.valueOf(backendCategoryId))
                        .param("jobType", "FULL_TIME")
                        .param("salaryMin", "2500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(seniorBackendJobId.toString()));
    }

    @Test
    void pagesTheFilteredResultWithoutRepeatingOrLosingAJob() throws Exception {
        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("keyword", marker)
                        .param("size", "3")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(4))
                .andExpect(jsonPath("$.data.page.totalPages").value(2))
                .andExpect(jsonPath("$.data.page.number").value(0))
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].id").value(internJobId.toString()));

        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("keyword", marker)
                        .param("size", "3")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.number").value(1))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(seniorBackendJobId.toString()));
    }

    @Test
    void sortingByHighestSalaryKeepsTheJobsWithoutOneAtTheBottom() throws Exception {
        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("keyword", marker)
                        .param("sort", "salaryMax,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(seniorBackendJobId.toString()))
                .andExpect(jsonPath("$.data.content[1].id").value(juniorBackendJobId.toString()))
                // Neither of these two names a salary, so they come last
                // whatever the database's default null ordering is.
                .andExpect(jsonPath("$.data.content[*].id")
                        .value(org.hamcrest.Matchers.hasItems(
                                remoteDesignJobId.toString(),
                                internJobId.toString()
                        )));
    }

    @Test
    void facetsCountEveryOptionTheMatchingJobsActuallyCarry() throws Exception {
        mockMvc.perform(get("/api/v1/public/jobs/facets").param("keyword", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalJobs").value(4))
                // "full_time" and "FULL_TIME" are the same option, not two.
                .andExpect(jsonPath("$.data.jobTypes[?(@.value == 'FULL_TIME')].count").value(2))
                .andExpect(jsonPath("$.data.jobTypes[?(@.value == 'PART_TIME')].count").value(1))
                .andExpect(jsonPath("$.data.workModes[?(@.value == 'REMOTE')].count").value(1))
                .andExpect(jsonPath("$.data.experienceLevels[?(@.value == 'SENIOR')].count").value(1))
                .andExpect(jsonPath("$.data.categories[?(@.id == '%s')].count".formatted(backendCategoryId)).value(2))
                // The job holding both skills counts once against each, never
                // twice against either.
                .andExpect(jsonPath("$.data.skills[?(@.id == '%s')].count".formatted(javaSkillId)).value(2))
                .andExpect(jsonPath("$.data.skills[?(@.id == '%s')].count".formatted(reactSkillId)).value(1))
                .andExpect(jsonPath("$.data.salaryRange.min").value(500))
                .andExpect(jsonPath("$.data.salaryRange.max").value(3000))
                .andExpect(jsonPath("$.data.postedWithin[?(@.value == '7')].count").value(4))
                // Nothing went up in the last day, so that window is not offered.
                .andExpect(jsonPath("$.data.postedWithin[?(@.value == '1')]").isEmpty());
    }

    @Test
    void aTickedFilterNarrowsTheOtherGroupsButNotItsOwn() throws Exception {
        mockMvc.perform(get("/api/v1/public/jobs/facets")
                        .param("keyword", marker)
                        .param("jobType", "FULL_TIME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalJobs").value(2))
                // Its own group keeps every option, or the sidebar could never
                // be used to widen the search again.
                .andExpect(jsonPath("$.data.jobTypes[?(@.value == 'INTERNSHIP')].count").value(1))
                .andExpect(jsonPath("$.data.jobTypes[?(@.value == 'FULL_TIME')].count").value(2))
                // Every other group describes the two full-time jobs only.
                .andExpect(jsonPath("$.data.categories.length()").value(2))
                .andExpect(jsonPath("$.data.categories[?(@.id == '%s')].count".formatted(backendCategoryId)).value(1))
                .andExpect(jsonPath("$.data.experienceLevels[?(@.value == 'JUNIOR')]").isEmpty());
    }

    @Test
    void postedWithinDaysKeepsOnlyTheRecentJobs() throws Exception {
        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("keyword", marker)
                        .param("postedWithinDays", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(internJobId.toString()));

        mockMvc.perform(get("/api/v1/public/jobs").param("postedWithinDays", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("postedWithinDays must be at least 1"));
    }

    @Test
    void rejectsASalaryRangeThatCannotMatchAnything() throws Exception {
        mockMvc.perform(get("/api/v1/public/jobs")
                        .param("salaryMin", "3000")
                        .param("salaryMax", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("salaryMin must be less than or equal to salaryMax"));

        mockMvc.perform(get("/api/v1/public/jobs").param("salaryMin", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Salary filters must not be negative"));
    }

    @Test
    void parentFiltersIncludeTheirSubcategoriesWithoutDuplicatingJobs() throws Exception {
        mockMvc.perform(get("/api/v1/public/jobs").param("keyword", marker)
                        .param("categoryId", backendParentId.toString(), backendCategoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(2));
        mockMvc.perform(get("/api/v1/public/jobs").param("keyword", marker)
                        .param("skillIds", javaParentId.toString(), javaSkillId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page.totalElements").value(2));
    }

    private JobCategory persistCategory(String name) {
        JobCategory parent = new JobCategory();
        parent.setName("Parent " + name);
        entityManager.persist(parent);
        JobCategory category = new JobCategory();
        category.setParent(parent);
        category.setName(name);
        entityManager.persist(category);
        return category;
    }

    private Skill persistSkill(String name) {
        Skill parent = new Skill();
        parent.setName("Parent " + name);
        entityManager.persist(parent);
        Skill skill = new Skill();
        skill.setParent(parent);
        skill.setName(name);
        skill.setSkillType("TECHNICAL");
        entityManager.persist(skill);
        return skill;
    }

    private JobPost persistJob(
            RecruiterProfile recruiterProfile,
            Company company,
            JobCategory category,
            String title,
            String jobType,
            String workMode,
            String experienceLevel,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            int daysOld
    ) {
        JobPost jobPost = new JobPost();
        jobPost.setRecruiterProfile(recruiterProfile);
        jobPost.setCompany(company);
        jobPost.setCategory(category);
        jobPost.setTitle(title);
        jobPost.setDescription("Description for " + title);
        jobPost.setLocation("Phnom Penh");
        jobPost.setJobType(jobType);
        jobPost.setWorkMode(workMode);
        jobPost.setExperienceLevel(experienceLevel);
        jobPost.setSalaryMin(salaryMin);
        jobPost.setSalaryMax(salaryMax);
        jobPost.setStatus(JobStatus.PUBLISHED);
        jobPost.setPublishedAt(Instant.now().minus(daysOld, ChronoUnit.DAYS));
        jobPost.setExpiredAt(Instant.now().plus(30, ChronoUnit.DAYS));
        entityManager.persist(jobPost);
        return jobPost;
    }

    private void attachSkill(JobPost jobPost, Skill skill) {
        JobPostSkill jobPostSkill = new JobPostSkill();
        jobPostSkill.setJobPost(jobPost);
        jobPostSkill.setSkill(skill);
        entityManager.persist(jobPostSkill);
    }
}
