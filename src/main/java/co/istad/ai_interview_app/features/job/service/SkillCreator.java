package co.istad.ai_interview_app.features.job.service;

import co.istad.ai_interview_app.features.job.entity.Skill;
import co.istad.ai_interview_app.features.job.repository.SkillRepository;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inserts one skill in a transaction of its own.
 *
 * <p>Separate bean, and {@code REQUIRES_NEW}, on purpose: two recruiters can
 * name the same new skill at the same moment, and the unique index will reject
 * the loser. Recovering from that means reading the winner's row afterwards,
 * which is impossible inside a transaction the violation has already marked
 * rollback-only. Isolating the insert keeps the caller's transaction usable.
 */
@Component
@RequiredArgsConstructor
public class SkillCreator {

    private final SkillRepository skillRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Skill create(
            String name,
            String skillType,
            RecruiterProfile createdBy
    ) {
        return create(name, skillType, createdBy, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Skill create(String name, String skillType, RecruiterProfile createdBy, UUID parentId) {
        Skill parent = parentId == null
                ? skillRepository.findByIdForUpdate(UUID.fromString("a7210000-0000-4000-8000-000000000003"))
                    .or(() -> skillRepository.findFirstByParentIsNullOrderByCreatedAtAsc())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ask an administrator to add a parent category before adding skills."))
                : skillRepository.findByIdForUpdate(parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent category was not found."));
        if (parent.getParent() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a parent category, not a subcategory.");
        }
        Skill skill = new Skill();
        skill.setParent(parent);
        skill.setName(name);
        skill.setSkillType(skillType);
        // Stamped so admins reviewing the shared list can tell a recruiter's
        // entry from one of their own, and see whose it is.
        skill.setCreatedByRecruiterProfile(createdBy);

        return skillRepository.saveAndFlush(skill);
    }
}
