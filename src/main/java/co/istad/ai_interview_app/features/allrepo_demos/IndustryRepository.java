package co.istad.ai_interview_app.features.allrepo_demos;

import co.istad.ai_interview_app.features.job_recruiter.domain.Industry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface IndustryRepository extends JpaRepository<Industry, Long> {
}