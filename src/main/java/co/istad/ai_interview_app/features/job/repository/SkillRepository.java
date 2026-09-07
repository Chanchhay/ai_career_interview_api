package co.istad.ai_interview_app.features.job.repository;

import co.istad.ai_interview_app.features.job.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {

    @EntityGraph(attributePaths = "parent")
    List<Skill> findAllByOrderByNameAsc();

    boolean existsByParent_Id(UUID parentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Skill> findFirstByParentIsNullOrderByCreatedAtAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from Skill item where item.id = :id")
    Optional<Skill> findByIdForUpdate(UUID id);

    /**
     * Looks skills up by name without caring about case, for matching the names
     * an AI lifted out of an uploaded job description.
     *
     * @param lowercaseNames names already lowercased by the caller
     */
    @Query("SELECT s FROM Skill s WHERE LOWER(s.name) IN :lowercaseNames")
    List<Skill> findAllByLowercaseNameIn(
            @Param("lowercaseNames") Collection<String> lowercaseNames
    );

    Optional<Skill> findFirstByNameIgnoreCase(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);
}
