package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.finance.entity.HiringRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface HiringRecordRepository extends JpaRepository<HiringRecord, Long> {

    Optional<HiringRecord> findByApplication_Id(Long applicationId);

    List<HiringRecord> findByCompany_Id(Long companyId);

    List<HiringRecord> findByJobPost_Id(Long jobPostId);

    List<HiringRecord> findByJobSeekerProfile_Id(Long jobSeekerProfileId);
}