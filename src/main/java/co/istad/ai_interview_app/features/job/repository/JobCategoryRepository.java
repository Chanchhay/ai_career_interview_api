package co.istad.ai_interview_app.features.job.repository;

import co.istad.ai_interview_app.features.job.entity.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobCategoryRepository extends JpaRepository<JobCategory, UUID> {

    @EntityGraph(attributePaths = "parent")
    List<JobCategory> findAllByOrderByNameAsc();

    boolean existsByParent_Id(UUID parentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from JobCategory item where item.id = :id")
    Optional<JobCategory> findByIdForUpdate(UUID id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);
}
