package co.istad.ai_interview_app.features.admin.service;

import co.istad.ai_interview_app.features.company.dto.IndustryCreateRequest;
import co.istad.ai_interview_app.features.company.dto.IndustryResponse;
import co.istad.ai_interview_app.features.company.dto.IndustryUpdateRequest;
import co.istad.ai_interview_app.features.company.entity.Industry;
import co.istad.ai_interview_app.features.company.repository.IndustryRepository;
import co.istad.ai_interview_app.features.job.dto.JobCategoryCreateRequest;
import co.istad.ai_interview_app.features.job.dto.JobCategoryResponse;
import co.istad.ai_interview_app.features.job.dto.JobCategoryUpdateRequest;
import co.istad.ai_interview_app.features.job.dto.SkillCreateRequest;
import co.istad.ai_interview_app.features.job.dto.SkillResponse;
import co.istad.ai_interview_app.features.job.dto.SkillUpdateRequest;
import co.istad.ai_interview_app.features.job.entity.JobCategory;
import co.istad.ai_interview_app.features.job.entity.Skill;
import co.istad.ai_interview_app.features.job.repository.JobCategoryRepository;
import co.istad.ai_interview_app.features.job.repository.SkillRepository;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static co.istad.ai_interview_app.shared.util.TextUtils.normalizeBlankToNull;

@Service
@RequiredArgsConstructor
public class AdminMetadataServiceImpl implements AdminMetadataService {

    private final JobCategoryRepository jobCategoryRepository;
    private final SkillRepository skillRepository;
    private final IndustryRepository industryRepository;

    // --- Job Category Operations ---

    @Override
    @Transactional
    public JobCategoryResponse createJobCategory(JobCategoryCreateRequest request) {
        String name = normalizeBlankToNull(request.name());
        if (name != null && jobCategoryRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job category with name '" + name + "' already exists");
        }

        JobCategory category = new JobCategory();
        category.setName(name);
        category.setDescription(normalizeBlankToNull(request.description()));

        JobCategory saved = jobCategoryRepository.save(category);
        return toCategoryResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobCategoryResponse> getAllJobCategories() {
        return jobCategoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JobCategoryResponse getJobCategoryById(Long id) {
        JobCategory category = jobCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job category not found with id " + id));
        return toCategoryResponse(category);
    }

    @Override
    @Transactional
    public JobCategoryResponse updateJobCategory(Long id, JobCategoryUpdateRequest request) {
        JobCategory category = jobCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job category not found with id " + id));

        String name = normalizeBlankToNull(request.name());
        if (name != null && jobCategoryRepository.existsByNameAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job category with name '" + name + "' already exists");
        }

        category.setName(name);
        category.setDescription(normalizeBlankToNull(request.description()));

        return toCategoryResponse(category);
    }

    @Override
    @Transactional
    public void deleteJobCategory(Long id) {
        JobCategory category = jobCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job category not found with id " + id));
        jobCategoryRepository.delete(category);
    }

    // --- Skill Operations ---

    @Override
    @Transactional
    public SkillResponse createSkill(SkillCreateRequest request) {
        String name = normalizeBlankToNull(request.name());
        if (name != null && skillRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Skill with name '" + name + "' already exists");
        }

        Skill skill = new Skill();
        skill.setName(name);
        skill.setSkillType(normalizeBlankToNull(request.skillType()));

        Skill saved = skillRepository.save(skill);
        return toSkillResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillResponse> getAllSkills() {
        return skillRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toSkillResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SkillResponse getSkillById(Long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found with id " + id));
        return toSkillResponse(skill);
    }

    @Override
    @Transactional
    public SkillResponse updateSkill(Long id, SkillUpdateRequest request) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found with id " + id));

        String name = normalizeBlankToNull(request.name());
        if (name != null && skillRepository.existsByNameAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Skill with name '" + name + "' already exists");
        }

        skill.setName(name);
        skill.setSkillType(normalizeBlankToNull(request.skillType()));

        return toSkillResponse(skill);
    }

    @Override
    @Transactional
    public void deleteSkill(Long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found with id " + id));
        skillRepository.delete(skill);
    }

    // --- Industry Operations ---

    @Override
    @Transactional
    public IndustryResponse createIndustry(IndustryCreateRequest request) {
        String name = normalizeBlankToNull(request.name());
        if (name != null && industryRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Industry with name '" + name + "' already exists");
        }

        Industry industry = new Industry();
        industry.setName(name);
        industry.setDescription(normalizeBlankToNull(request.description()));
        industry.setStatus(request.status() != null ? request.status() : ProfileStatus.ACTIVE);

        Industry saved = industryRepository.save(industry);
        return toIndustryResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndustryResponse> getAllIndustries() {
        return industryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toIndustryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public IndustryResponse getIndustryById(Long id) {
        Industry industry = industryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Industry not found with id " + id));
        return toIndustryResponse(industry);
    }

    @Override
    @Transactional
    public IndustryResponse updateIndustry(Long id, IndustryUpdateRequest request) {
        Industry industry = industryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Industry not found with id " + id));

        String name = normalizeBlankToNull(request.name());
        if (name != null && industryRepository.existsByNameAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Industry with name '" + name + "' already exists");
        }

        industry.setName(name);
        industry.setDescription(normalizeBlankToNull(request.description()));
        if (request.status() != null) {
            industry.setStatus(request.status());
        }

        return toIndustryResponse(industry);
    }

    @Override
    @Transactional
    public void deleteIndustry(Long id) {
        Industry industry = industryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Industry not found with id " + id));
        industryRepository.delete(industry);
    }

    // --- Mappers ---

    private JobCategoryResponse toCategoryResponse(JobCategory category) {
        return new JobCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private SkillResponse toSkillResponse(Skill skill) {
        return new SkillResponse(
                skill.getId(),
                skill.getName(),
                skill.getSkillType(),
                skill.getCreatedAt(),
                skill.getUpdatedAt()
        );
    }

    private IndustryResponse toIndustryResponse(Industry industry) {
        return new IndustryResponse(
                industry.getId(),
                industry.getName(),
                industry.getDescription(),
                industry.getStatus(),
                industry.getCreatedAt(),
                industry.getUpdatedAt()
        );
    }
}
