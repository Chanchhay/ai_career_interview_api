package co.istad.ai_interview_app.features.job;

import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.company.repository.CompanyRepository;
import co.istad.ai_interview_app.features.job.dto.ResolvedSkill;
import co.istad.ai_interview_app.features.job.dto.SkillCreateRequest;
import co.istad.ai_interview_app.features.job.dto.SkillResponse;
import co.istad.ai_interview_app.features.job.entity.Skill;
import co.istad.ai_interview_app.features.job.mapper.SkillMapper;
import co.istad.ai_interview_app.features.job.repository.SkillRepository;
import co.istad.ai_interview_app.features.job.service.RecruiterSkillServiceImpl;
import co.istad.ai_interview_app.features.job.service.SkillCreator;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.recruiter.service.AuthenticatedRecruiterProfileResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Recruiters add skills two ways — by hand, and by importing a job description
 * that names them. The rules that keep either from turning the shared taxonomy
 * into a pile of near-duplicates live in {@code findOrCreateAll}.
 */
class RecruiterSkillServiceTest {

    private SkillRepository skillRepository;
    private SkillCreator skillCreator;
    private RecruiterProfile recruiter;
    private RecruiterSkillServiceImpl service;

    @BeforeEach
    void setUp() {
        skillRepository = mock(SkillRepository.class);
        skillCreator = mock(SkillCreator.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);

        recruiter = new RecruiterProfile();
        recruiter.setId(11L);

        AuthenticatedRecruiterProfileResolver resolver =
                mock(AuthenticatedRecruiterProfileResolver.class);
        when(resolver.resolve()).thenReturn(recruiter);

        Company company = new Company();
        company.setName("Acme Ltd");
        company.setRecruiterProfile(recruiter);
        when(companyRepository.findFirstByRecruiterProfile_Id(11L))
                .thenReturn(Optional.of(company));

        service = new RecruiterSkillServiceImpl(
                skillRepository,
                skillCreator,
                resolver,
                new SkillMapper(companyRepository)
        );

        when(skillRepository.findAllByLowercaseNameIn(any())).thenReturn(List.of());
        when(skillRepository.findFirstByNameIgnoreCase(any())).thenReturn(Optional.empty());
        when(skillCreator.create(any(), any(), any())).thenAnswer(invocation -> skill(
                42L,
                invocation.getArgument(0),
                invocation.getArgument(1),
                invocation.getArgument(2)
        ));
    }

    private static Skill skill(Long id, String name, String skillType) {
        return skill(id, name, skillType, null);
    }

    private static Skill skill(
            Long id,
            String name,
            String skillType,
            RecruiterProfile createdBy
    ) {
        Skill skill = new Skill();
        skill.setId(id);
        skill.setName(name);
        skill.setSkillType(skillType);
        skill.setCreatedByRecruiterProfile(createdBy);
        return skill;
    }

    @Test
    void anExistingSkillIsReturnedRatherThanDuplicated() {
        when(skillRepository.findAllByLowercaseNameIn(any()))
                .thenReturn(List.of(skill(3L, "React", "LIBRARY")));

        SkillResponse response = service.findOrCreate(
                new SkillCreateRequest("react", "FRAMEWORK")
        );

        assertThat(response.id()).isEqualTo(3L);
        // The stored name and type win: the recruiter is attaching a skill, not
        // reclassifying one that already exists.
        assertThat(response.name()).isEqualTo("React");
        assertThat(response.skillType()).isEqualTo("LIBRARY");
        // An admin-entered skill stays unattributed when a recruiter reuses it.
        assertThat(response.createdByRecruiterProfileId()).isNull();
        verify(skillCreator, never()).create(any(), any(), any());
    }

    @Test
    void aNewSkillIsCreatedWithItsNameAndTypeNormalizedAndAttributed() {
        SkillResponse response = service.findOrCreate(
                new SkillCreateRequest("  React   Native ", "framework")
        );

        verify(skillCreator).create("React Native", "FRAMEWORK", recruiter);
        assertThat(response.id()).isEqualTo(42L);
        // Admins can see this did not come from them, and whose it is.
        assertThat(response.createdByRecruiterProfileId()).isEqualTo(11L);
        assertThat(response.createdByCompanyName()).isEqualTo("Acme Ltd");
    }

    @Test
    void aSkillWithoutATypeIsAllowed() {
        service.findOrCreate(new SkillCreateRequest("Zustand", "  "));

        verify(skillCreator).create(eq("Zustand"), eq(null), any());
    }

    @Test
    void aBlankNameIsRejected() {
        assertThatThrownBy(() -> service.findOrCreate(new SkillCreateRequest("   ", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);

        verify(skillCreator, never()).create(any(), any(), any());
    }

    @Test
    void importingAJobResolvesEveryNameInOnePass() {
        when(skillRepository.findAllByLowercaseNameIn(any()))
                .thenReturn(List.of(skill(3L, "React", "LIBRARY")));

        List<ResolvedSkill> resolved = service.findOrCreateAll(List.of(
                new SkillCreateRequest("React", "LIBRARY"),
                // The same skill in another casing, and a blank: neither should
                // reach the table.
                new SkillCreateRequest("react", "LIBRARY"),
                new SkillCreateRequest("  ", null),
                new SkillCreateRequest("Zustand", "LIBRARY")
        ));

        assertThat(resolved).hasSize(2);
        assertThat(resolved.get(0).created()).isFalse();
        assertThat(resolved.get(0).skill().name()).isEqualTo("React");
        assertThat(resolved.get(1).created()).isTrue();
        assertThat(resolved.get(1).skill().name()).isEqualTo("Zustand");

        // One lookup for the whole batch, and only the missing name is created.
        verify(skillRepository).findAllByLowercaseNameIn(any());
        verify(skillCreator).create("Zustand", "LIBRARY", recruiter);
    }

    @Test
    void losingARaceToCreateTheSameSkillReadsBackTheWinnersRow() {
        when(skillCreator.create(any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(skillRepository.findFirstByNameIgnoreCase("Zustand"))
                .thenReturn(Optional.of(skill(9L, "Zustand", "LIBRARY")));

        List<ResolvedSkill> resolved = service.findOrCreateAll(
                List.of(new SkillCreateRequest("Zustand", "LIBRARY"))
        );

        assertThat(resolved).hasSize(1);
        assertThat(resolved.getFirst().skill().id()).isEqualTo(9L);
        // Someone else's row, so this import did not create it.
        assertThat(resolved.getFirst().created()).isFalse();
    }
}
