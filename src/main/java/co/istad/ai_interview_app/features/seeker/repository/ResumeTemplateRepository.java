package co.istad.ai_interview_app.features.seeker.repository;

import co.istad.ai_interview_app.features.seeker.entity.ResumeTemplate;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeTemplateRepository extends JpaRepository<ResumeTemplate, Long> {

    List<ResumeTemplate> findAllByStatusOrderByNameAsc(ProfileStatus status);

    Optional<ResumeTemplate> findByIdAndStatus(Long id, ProfileStatus status);
}
