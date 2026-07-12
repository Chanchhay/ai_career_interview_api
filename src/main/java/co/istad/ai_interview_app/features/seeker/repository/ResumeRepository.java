package co.istad.ai_interview_app.features.seeker.repository;

import co.istad.ai_interview_app.features.seeker.entity.Resume;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByIdAndJobSeekerProfile_Id(Long id, Long jobSeekerProfileId);

    List<Resume> findAllByJobSeekerProfile_IdAndVisibilityOrderByPublishedAtDescCreatedAtDesc(
            Long jobSeekerProfileId,
            VisibilityStatus visibility
    );
}
