package co.istad.ai_interview_app.features.company.repository;

import co.istad.ai_interview_app.features.company.entity.Industry;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndustryRepository extends JpaRepository<Industry, Long> {

    List<Industry> findAllByStatusOrderByNameAsc(ProfileStatus status);
}
