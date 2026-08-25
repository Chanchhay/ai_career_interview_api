package co.istad.ai_interview_app.features.job.mapper;

import co.istad.ai_interview_app.features.company.entity.Company;
import co.istad.ai_interview_app.features.company.service.CompanyIdentity;
import co.istad.ai_interview_app.features.company.repository.CompanyRepository;
import co.istad.ai_interview_app.features.job.dto.SkillResponse;
import co.istad.ai_interview_app.features.job.entity.Skill;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds skill responses, naming the recruiter behind any skill that did not
 * come from an admin.
 *
 * <p>The name is the recruiter's company rather than the person: identity lives
 * in Keycloak, and "Acme Ltd" tells a reviewing admin more than a user id would.
 */
@Component
@RequiredArgsConstructor
public class SkillMapper {

    private final CompanyRepository companyRepository;

    public SkillResponse toResponse(Skill skill) {
        Long recruiterProfileId = authorId(skill);

        String companyName = recruiterProfileId == null
                ? null
                : companyRepository.findFirstByRecruiterProfile_Id(recruiterProfileId)
                        // Masked here too: /public/skills is open to anyone, and
                        // "who added this skill" would otherwise name a company
                        // that every job listing is careful not to.
                        .map(CompanyIdentity::displayName)
                        .orElse(null);

        return toResponse(skill, recruiterProfileId, companyName);
    }

    /**
     * List form. Resolves every author's company in one query rather than one
     * per skill, since the admin list is the main caller.
     */
    public List<SkillResponse> toResponses(List<Skill> skills) {
        Set<Long> recruiterProfileIds = new HashSet<>();
        for (Skill skill : skills) {
            Long recruiterProfileId = authorId(skill);
            if (recruiterProfileId != null) {
                recruiterProfileIds.add(recruiterProfileId);
            }
        }

        Map<Long, String> companyNamesByProfileId = new HashMap<>();
        if (!recruiterProfileIds.isEmpty()) {
            for (Company company : companyRepository.findAllByRecruiterProfile_IdIn(recruiterProfileIds)) {
                companyNamesByProfileId.putIfAbsent(
                        company.getRecruiterProfile().getId(),
                        CompanyIdentity.displayName(company)
                );
            }
        }

        return skills.stream()
                .map(skill -> {
                    Long recruiterProfileId = authorId(skill);
                    return toResponse(
                            skill,
                            recruiterProfileId,
                            recruiterProfileId == null
                                    ? null
                                    : companyNamesByProfileId.get(recruiterProfileId)
                    );
                })
                .toList();
    }

    private Long authorId(Skill skill) {
        RecruiterProfile author = skill.getCreatedByRecruiterProfile();

        return author == null ? null : author.getId();
    }

    private SkillResponse toResponse(
            Skill skill,
            Long recruiterProfileId,
            String companyName
    ) {
        return new SkillResponse(
                skill.getId(),
                skill.getName(),
                skill.getSkillType(),
                recruiterProfileId,
                companyName,
                skill.getCreatedAt(),
                skill.getUpdatedAt()
        );
    }
}
