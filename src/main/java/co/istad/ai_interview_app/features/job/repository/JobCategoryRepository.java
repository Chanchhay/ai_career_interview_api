package co.istad.ai_interview_app.features.job.repository;

import co.istad.ai_interview_app.features.job.entity.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobCategoryRepository extends JpaRepository<JobCategory, Long> {

    List<JobCategory> findAllByOrderByNameAsc();
}
