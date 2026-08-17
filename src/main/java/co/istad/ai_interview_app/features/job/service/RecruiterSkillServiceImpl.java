package co.istad.ai_interview_app.features.job.service;

import co.istad.ai_interview_app.features.job.dto.ResolvedSkill;
import co.istad.ai_interview_app.features.job.dto.SkillCreateRequest;
import co.istad.ai_interview_app.features.job.dto.SkillResponse;
import co.istad.ai_interview_app.features.job.entity.Skill;
import co.istad.ai_interview_app.features.job.mapper.SkillMapper;
import co.istad.ai_interview_app.features.job.repository.SkillRepository;
import co.istad.ai_interview_app.features.recruiter.entity.RecruiterProfile;
import co.istad.ai_interview_app.features.recruiter.service.AuthenticatedRecruiterProfileResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Service
@RequiredArgsConstructor
public class RecruiterSkillServiceImpl implements RecruiterSkillService {

    private final SkillRepository skillRepository;
    private final SkillCreator skillCreator;
    private final AuthenticatedRecruiterProfileResolver recruiterProfileResolver;
    private final SkillMapper skillMapper;

    @Override
    @Transactional
    public SkillResponse findOrCreate(SkillCreateRequest request) {
        List<ResolvedSkill> resolved = findOrCreateAll(List.of(request));
        if (resolved.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Skill name is required"
            );
        }

        return resolved.getFirst().skill();
    }

    @Override
    @Transactional
    public List<ResolvedSkill> findOrCreateAll(List<SkillCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        // Keyed by lowercase name, so a document naming both "React" and
        // "react" produces one skill rather than a duplicate row.
        Map<String, SkillCreateRequest> requestedByKey = new LinkedHashMap<>();
        for (SkillCreateRequest request : requests) {
            if (request == null) {
                continue;
            }

            String name = normalizeName(request.name());
            if (name != null) {
                requestedByKey.putIfAbsent(
                        name.toLowerCase(Locale.ROOT),
                        new SkillCreateRequest(name, request.skillType())
                );
            }
        }

        if (requestedByKey.isEmpty()) {
            return List.of();
        }

        Map<String, Skill> existingByKey = new LinkedHashMap<>();
        for (Skill skill : skillRepository.findAllByLowercaseNameIn(requestedByKey.keySet())) {
            if (skill.getName() != null) {
                existingByKey.putIfAbsent(skill.getName().toLowerCase(Locale.ROOT), skill);
            }
        }

        List<ResolvedSkill> resolved = new ArrayList<>();
        // Looked up only if something actually has to be created, so resolving
        // skills a recruiter already has costs no extra query.
        RecruiterProfile author = null;

        for (Map.Entry<String, SkillCreateRequest> entry : requestedByKey.entrySet()) {
            Skill existing = existingByKey.get(entry.getKey());
            if (existing != null) {
                resolved.add(new ResolvedSkill(skillMapper.toResponse(existing), false));
                continue;
            }

            if (author == null) {
                author = recruiterProfileResolver.resolve();
            }

            SkillCreateRequest request = entry.getValue();
            resolved.add(create(request.name(), request.skillType(), author));
        }

        return List.copyOf(resolved);
    }

    private ResolvedSkill create(
            String name,
            String skillType,
            RecruiterProfile author
    ) {
        try {
            Skill created = skillCreator.create(name, normalizeType(skillType), author);
            return new ResolvedSkill(skillMapper.toResponse(created), true);
        } catch (DataIntegrityViolationException e) {
            // Another recruiter named the same new skill first. The insert ran
            // in its own transaction, so this one is still usable and can read
            // back the row that won.
            return skillRepository.findFirstByNameIgnoreCase(name)
                    .map(skill -> new ResolvedSkill(skillMapper.toResponse(skill), false))
                    .orElseThrow(() -> e);
        }
    }

    /** Collapses inner whitespace so "  React   Native " cannot shadow "React Native". */
    private String normalizeName(String value) {
        String normalized = normalizeBlankToNull(value);

        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    private String normalizeType(String value) {
        String normalized = normalizeBlankToNull(value);

        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
