package co.istad.ai_interview_app.features.company.repository;

import co.istad.ai_interview_app.features.company.entity.Industry;
import co.istad.ai_interview_app.shared.enums.profile.ProfileStatus;
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
public interface IndustryRepository extends JpaRepository<Industry, UUID> {

    @EntityGraph(attributePaths = "parent")
    List<Industry> findAllByStatusOrderByNameAsc(ProfileStatus status);

    @EntityGraph(attributePaths = "parent")
    List<Industry> findAllByOrderByNameAsc();

    boolean existsByParent_Id(UUID parentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from Industry item where item.id = :id")
    Optional<Industry> findByIdForUpdate(UUID id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);
}
