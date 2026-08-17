package co.istad.ai_interview_app.features.job.service;

import co.istad.ai_interview_app.features.job.dto.ResolvedSkill;
import co.istad.ai_interview_app.features.job.dto.SkillCreateRequest;
import co.istad.ai_interview_app.features.job.dto.SkillResponse;

import java.util.List;

public interface RecruiterSkillService {

    /**
     * Returns the skill by this name, creating it when it does not exist yet.
     *
     * <p>Idempotent by design: a recruiter attaching "React" to a job wants its
     * id, and whether they happen to be the first person to name it is not
     * their problem. Matching ignores case so the table does not collect
     * "react", "React" and "REACT" as separate rows.
     */
    SkillResponse findOrCreate(SkillCreateRequest request);

    /**
     * Batch form, used when importing a job description: every skill the
     * document names is resolved in one pass, existing rows reused and the rest
     * created.
     *
     * <p>Duplicates within the input collapse, blank names are skipped, and the
     * result keeps the order the names arrived in. Each entry says whether it
     * was created, so the recruiter can be shown what their upload added.
     */
    List<ResolvedSkill> findOrCreateAll(List<SkillCreateRequest> requests);
}
