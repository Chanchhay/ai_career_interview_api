package co.istad.ai_interview_app.features.seeker.repository;

import co.istad.ai_interview_app.features.seeker.entity.Resume;
import co.istad.ai_interview_app.shared.enums.visibility.VisibilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByIdAndJobSeekerProfile_Id(Long id, Long jobSeekerProfileId);

    List<Resume> findAllByJobSeekerProfile_IdOrderByCreatedAtDesc(Long jobSeekerProfileId);

    List<Resume> findAllByJobSeekerProfile_IdAndVisibilityOrderByPublishedAtDescCreatedAtDesc(
            Long jobSeekerProfileId,
            VisibilityStatus visibility
    );

    @Modifying
    @Query("""
            update Resume resume
            set resume.isDefault = false
            where resume.jobSeekerProfile.id = :jobSeekerProfileId
            """)
    int clearDefaultForJobSeekerProfile(Long jobSeekerProfileId);
}
